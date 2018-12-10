"""
Simple REST Example using Flask-Restful Library.  Works on python 2.7 and 3.7.

This is a stateless example - No Global Variables are used.
--> All APIs must pass in the filename
--> Task APIs need filename, task, operation

Started with Example from  https://flask-restful.readthedocs.io/en/0.3.5/quickstart.html
Must activate the virtualenv for python 3:  source env/bin/activate


// Get task files
curl http://127.0.0.1:5000/taskfiles -X GET


// Get all tasks
curl http://127.0.0.1:5000/tasks -X POST -v -H "Content-type: application/json" -d "{\"filename\": \"t1.ta\", \"operation\": \"GET\"}"


// Get a task
curl http://127.0.0.1:5000/task -X POST -v -H "Content-type: application/json" -d "{\"filename\": \"t1.ta\", \"taskid\": \"task1\", \"operation\": \"GET\"}"

// Delete a task
curl http://127.0.0.1:5000/task -X POST -v -H "Content-type: application/json" -d "{\"filename\": \"t1.ta\", \"taskid\": \"task4\", \"operation\": \"DELETE\"}"

// Add or Update a task
curl http://127.0.0.1:5000/task -X POST -v -H "Content-type: application/json" -d "{\"filename\": \"t1.ta\", \"taskid\": \"task4\", \"operation\": \"PUT\", \"desc\": \"Re-Add 4th Task.\", \"dur\": \"77\"}"

"""
from flask import Flask
from flask_restful import reqparse, abort, Api, Resource, fields, marshal_with, marshal
import json
import os

app = Flask(__name__)
api = Api(app)

OPERATION_GET = "GET"
OPERATION_PUT = "PUT"
OPERATION_DELETE = "DELETE"
MESSAGE_SUCCESS = {"message": "success"}
MESSAGE_UNKNOWN_OPERATION = {"message": "Unknown Operation"}

#
# TASK MODULE
#

parser = reqparse.RequestParser()
parser.add_argument('filename')
parser.add_argument('operation')
parser.add_argument('taskid')
parser.add_argument('desc')
parser.add_argument('dur')
parser.add_argument('tasklist', action='append')

# python2 / python3
def get_dict_values_as_list(dict):
    olist = []
    for v in dict.values():
        olist.append(v)
    return olist

task_fields = {
    'id':   fields.String,
    'desc': fields.String,
    'dur': fields.Integer,
    'uri':  fields.Url('task_ep', absolute=True)
}

class Task(object):
    def __init__(self, id, desc, dur):
        self.id = id
        self.desc = desc
        self.dur = dur

def get_task(id, taskdict):
    return taskdict[id]

def remove_task(task, taskdict):
    del taskdict[task.id]

def task_exists(id, taskdict):
    if id in taskdict:
        return True
    else:
        return False

def create_new_task(taskdict, tid, tdesc, tdur):
    if tid in taskdict:
        abort(400, message="Task {} already exists".format(id))
    ta = Task(id=tid, desc=tdesc, dur=tdur)
    taskdict[ta.id] = ta
    return ta

#
# FILE MODULE
#

FEXTENSION = ".ta"
FDELIMITER = ","
NEWLINE = "\n"

file_fields = {
    'filename': fields.String
}

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
        filedict['filename'] = name
        fList.append(filedict)
    return fList

# Assumes each line represents a task in this form:  taskid, taskdesc, taskduration
def load_task_file(filename, taskdict):
    if (os.path.isfile(filename)):
        with open(filename, mode='rt') as filestream:
            for line in filestream:
                lstr = line.rstrip(NEWLINE)
                larray = lstr.split(FDELIMITER)
                create_new_task(taskdict, larray[0], larray[1], larray[2])

# Writes out task lines in this form:  taskid, taskdesc, taskduration
def write_task_file(filename, taskdict):
    with open(filename, mode='wt') as filestream:
        for task in taskdict.values():
            filestream.write(task.id)
            filestream.write(FDELIMITER)
            filestream.write(task.desc)
            filestream.write(FDELIMITER)
            filestream.write(task.dur)
            filestream.write(NEWLINE)

#
# REST MODULES
#

# TaskApi
# POST      - JSON Payload:  filename, taskid, operation
class TaskApi(Resource):
    def post(self, **kwargs):
        # Get arguments
        args = parser.parse_args()
        fname = args['filename']
        taskid = args['taskid']
        operation = args['operation']
        #
        # Get Task Data
        taskdict = {}
        load_task_file(fname, taskdict)
        taskexists = task_exists(taskid, taskdict)
        #
        # Execute Operation
        if (operation == OPERATION_PUT):
            taskdesc = args['desc']
            taskdur = args['dur']
            if (taskexists):
                # update existing task
                task = get_task(taskid, taskdict)
                task.desc = taskdesc
                task.dur = taskdur
            else:
                # create new task
                create_new_task(taskdict, taskid, taskdesc, taskdur)
            write_task_file(fname, taskdict)
            return MESSAGE_SUCCESS, 200
        elif (operation == OPERATION_GET):
            if (not taskexists):
                abort(404, message="Task {} doesn't exist".format(taskid))
            task = get_task(taskid, taskdict)
            return marshal(task, task_fields), 200
        elif (operation == OPERATION_DELETE):
            if (not taskexists):
                abort(404, message="Task {} doesn't exist".format(taskid))
            task = get_task(taskid, taskdict)
            remove_task(task, taskdict)
            write_task_file(fname, taskdict)
            return MESSAGE_SUCCESS, 200
        else:
            return MESSAGE_UNKNOWN_OPERATION, 200

# TaskListApi
# POST      - JSON Payload:  filename, operation
class TaskListApi(Resource):
    def post(self, **kwargs):
        args = parser.parse_args()
        fname = args['filename']
        operation = args['operation']
        taskdict = {}
        load_task_file(fname, taskdict)
        if (operation == OPERATION_GET):
            tasklist = get_dict_values_as_list(taskdict)
            return marshal(tasklist, task_fields), 200
        else:
            return MESSAGE_UNKNOWN_OPERATION, 200

# FileListApi
# GET       - List the task filenames
class FileListApi(Resource):
    @marshal_with(file_fields)
    def get(self, **kwargs):
        return get_file_list(), 200


## Api resource routing
api.add_resource(FileListApi, '/taskfiles', endpoint='filelist_ep')
api.add_resource(TaskListApi, '/tasks', endpoint='tasklist_ep')
api.add_resource(TaskApi, '/task', endpoint='task_ep')

if __name__ == '__main__':
    app.run(debug=True)
