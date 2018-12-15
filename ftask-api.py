"""
Simple REST Example using Flask-Restful Library.  Works on python 2.7 and 3.7.

This is a stateless example - No Global Variables are used.

Started with Example from  https://flask-restful.readthedocs.io/en/0.3.5/quickstart.html
Must activate the virtualenv for python 3:  source env/bin/activate

// Get tasks dataset list
curl http://127.0.0.1:5000/tasks -X GET

// Get all tasks by dataset
curl http://127.0.0.1:5000/tasks/t1 -X GET

// Get a task
curl http://127.0.0.1:5000/tasks/t1/task4 -X GET

// Delete a task
curl http://127.0.0.1:5000/tasks/t1/task4 -X DELETE

// Add or Update a task
curl http://127.0.0.1:5000/tasks/t1/task4 -X POST -v -H "Content-type: application/json" -d "{\"desc\": \"Re-Add 4th Task.\", \"dur\": \"77\"}"

"""
from flask import Flask
from flask_restful import reqparse, abort, Api, Resource, fields, marshal
import json
import os

app = Flask(__name__)
api = Api(app)

MESSAGE_SUCCESS = {"message": "success"}

#
# TASK MODULE
#

parser = reqparse.RequestParser()
parser.add_argument('desc')
parser.add_argument('dur')

def get_dict_values_as_list(dict):
    olist = []
    for v in dict.values():
        olist.append(v)
    return olist

task_fields = {
    'datasetid': fields.String,
    'taskid': fields.String,
    'desc': fields.String,
    'dur': fields.Integer,
    'uri':  fields.Url('task_ep', absolute=True)
}

class Task(object):
    def __init__(self, datasetid, taskid, desc, dur):
        self.datasetid = datasetid
        self.taskid = taskid
        self.desc = desc
        self.dur = dur

def get_task(id, taskdict):
    return taskdict[id]

def remove_task(task, taskdict):
    del taskdict[task.taskid]

def task_exists(id, taskdict):
    if id in taskdict:
        return True
    else:
        return False

def create_new_task(taskdict, did, tid, tdesc, tdur):
    if tid in taskdict:
        abort(400, message="Task {} already exists".format(id))
    ta = Task(datasetid=did, taskid=tid, desc=tdesc, dur=tdur)
    taskdict[ta.taskid] = ta
    return ta

#
# FILE MODULE
#

FEXTENSION = ".ta"
FDELIMITER = ","
NEWLINE = "\n"

file_fields = {
    'dataset': fields.String
}

def file_exists(filename):
    if (os.path.isfile(filename)):
        return True
    else:
        return False

def get_task_filenames():
    nlist = []
    cdir = os.getcwd()
    for fname in os.listdir(cdir):
        if fname.endswith(FEXTENSION):
            nlist.append(fname)
    return nlist

def get_file_list():
    fList = []
    nList = get_task_filenames()
    for name in nList:
        filedict = {}
        filedict['dataset'] = name
        fList.append(filedict)
    return fList

# Assumes each line represents a task in this form:  taskid, taskdesc, taskduration
def load_task_file(datasetid, taskdict):
    filename = datasetid + FEXTENSION
    if (os.path.isfile(filename)):
        with open(filename, mode='rt') as filestream:
            for line in filestream:
                lstr = line.rstrip(NEWLINE)
                larray = lstr.split(FDELIMITER)
                create_new_task(taskdict, datasetid, larray[0], larray[1], larray[2])

# Writes out task lines in this form:  taskid, taskdesc, taskduration
def write_task_file(filename, taskdict):
    with open(filename, mode='wt') as filestream:
        for task in taskdict.values():
            filestream.write(task.taskid)
            filestream.write(FDELIMITER)
            filestream.write(task.desc)
            filestream.write(FDELIMITER)
            filestream.write(task.dur)
            filestream.write(NEWLINE)

#
# REST MODULES
#

# TaskApi
# GET    - Get a task
# DELETE - Delete a task
# POST   - Add or Update a task
class TaskApi(Resource):
    def get(self, **kwargs):
        # get values and check for dataset existence
        datasetid = kwargs["datasetid"]
        taskid = kwargs["taskid"]
        fname = datasetid + FEXTENSION
        if (not file_exists(fname)):
            abort(404, message="Tasks Dataset {} does not exist".format(fname))

        # Get Task data and check for task existence
        taskdict = {}
        load_task_file(datasetid, taskdict)
        taskexists = task_exists(taskid, taskdict)
        if (not taskexists):
            abort(404, message="Task {} does not exist".format(taskid))

        # Return task
        task = get_task(taskid, taskdict)
        return marshal(task, task_fields), 200

    def delete(self, **kwargs):
        # get values and check for dataset existence
        datasetid = kwargs["datasetid"]
        taskid = kwargs["taskid"]
        fname = datasetid + FEXTENSION
        if (not file_exists(fname)):
            abort(404, message="Tasks Dataset {} does not exist".format(fname))

        # Get Task data and check for task existence
        taskdict = {}
        load_task_file(datasetid, taskdict)
        taskexists = task_exists(taskid, taskdict)
        if (not taskexists):
            abort(404, message="Task {} does not exist".format(taskid))

        # Delete Task and return
        task = get_task(taskid, taskdict)
        remove_task(task, taskdict)
        write_task_file(fname, taskdict)
        return MESSAGE_SUCCESS, 200

    def post(self, **kwargs):
        # get values and check for dataset existence
        args = parser.parse_args()
        taskdesc = args['desc']
        taskdur = args['dur']
        datasetid = kwargs["datasetid"]
        taskid = kwargs["taskid"]
        fname = datasetid + FEXTENSION
        if (not file_exists(fname)):
            abort(404, message="Tasks Dataset {} does not exist".format(fname))

        # Get Task data
        taskdict = {}
        load_task_file(datasetid, taskdict)
        taskexists = task_exists(taskid, taskdict)

        # Update if the task exists, otherwise add a new task
        if (taskexists):
            task = get_task(taskid, taskdict)
            task.desc = taskdesc
            task.dur = taskdur
        else:
            create_new_task(taskdict, datasetid, taskid, taskdesc, taskdur)

        # update dataset and return
        write_task_file(fname, taskdict)
        return MESSAGE_SUCCESS, 200

# TaskListApi
# GET - Get all tasks by dataset
class TaskListApi(Resource):
    def get(self, **kwargs):
        datasetid = kwargs["datasetid"]
        fname = datasetid + FEXTENSION
        if (not file_exists(fname)):
            abort(404, message="Tasks Dataset {} does not exist".format(fname))
        taskdict = {}
        load_task_file(datasetid, taskdict)
        tasklist = get_dict_values_as_list(taskdict)
        return marshal(tasklist, task_fields), 200

# GET - List the task dataset names
class TaskDatasetsApi(Resource):
    def get(self, **kwargs):
        return marshal(get_file_list(), file_fields), 200

## Api resource routing
api.add_resource(TaskDatasetsApi, '/tasks', endpoint='tasks_ep')
api.add_resource(TaskListApi, '/tasks/<datasetid>', endpoint='tasklist_ep')
api.add_resource(TaskApi, '/tasks/<datasetid>/<taskid>', endpoint='task_ep')

if __name__ == '__main__':
    app.run(debug=True)
