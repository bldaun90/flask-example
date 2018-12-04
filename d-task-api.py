"""
Simple REST Example using Flask-Restful Library.  Works on python 2.7 and 3.7.

Full Example from  https://flask-restful.readthedocs.io/en/0.3.5/quickstart.html
Must activate the virtualenv for python 3:  source env/bin/activate

// GET the list
curl http://127.0.0.1:5000/tasks -X GET

// GET a single task
curl http://127.0.0.1:5000/tasks/task3 -X GET

// DELETE a single task
curl http://127.0.0.1:5000/tasks/task2 -X DELETE -v

// DELETE All Tasks
curl http://127.0.0.1:5000/tasks -X DELETE -v

// PUT (Update) a task
curl http://127.0.0.1:5000/tasks/task3
curl http://127.0.0.1:5000/tasks/task3 -d "desc=Do Something Different" -X PUT -v

// POST (Add) a new task
curl http://127.0.0.1:5000/tasks -H "Content-type: application/json" -d "{\"taskid\": \"task1\", \"desc\": \"The 1st Task.\"}" -X POST -v

// PUT (Replace All) Tasks
curl http://127.0.0.1:5000/tasks -H "Content-type: application/json" -d "{\"tasklist\": [{\"taskid\": \"task4\", \"desc\": \"The Fourth Task\"}, {\"taskid\": \"task5\", \"desc\": \"The Fifth Task\"}]}" -X PUT -v

"""
from flask import Flask
from flask_restful import reqparse, abort, Api, Resource, fields, marshal_with
import json

app = Flask(__name__)
api = Api(app)

parser = reqparse.RequestParser()
parser.add_argument('taskid')
parser.add_argument('desc')
parser.add_argument('tasklist', action='append')

# Task Dictionary
TASKS = {}

# python2 / python3
def get_dict_values_as_list(dict):
    olist = []
    for v in dict.values():
        olist.append(v)
    return olist

# python2 / python3
def get_dict_keys_as_list(dict):
    olist = []
    for k in dict.keys():
        olist.append(k)
    return olist

task_fields = {
    'id':   fields.String,
    'desc': fields.String,
    'uri':  fields.Url('task_ep', absolute=True, scheme="http")
}

class Task(object):
    def __init__(self, id, desc):
        self.id = id
        self.desc = desc

def get_task(id):
    return TASKS[id]

def add_task(task):
    TASKS[task.id] = task

def remove_task(task):
    del TASKS[task.id]

# python2 / python3
def remove_all_tasks():
    tkeys = get_dict_keys_as_list(TASKS)
    for k in tkeys:
        del TASKS[k]

def abort_if_task_doesnt_exist(id):
    if id not in TASKS:
        abort(404, message="Task {} doesn't exist".format(id))

def create_new_task(tid, tdesc):
    if tid in TASKS:
        abort(400, message="Task {} already exists".format(tid))
    ta = Task(id=tid, desc=tdesc)
    add_task(ta)
    return ta

# python2 / python3
def get_task_list():
    return get_dict_values_as_list(TASKS)

# TaskApi
# GET       - Retrieve a representation of the addressed member of the collection.
# PUT       - Replace the addressed member of the collection - Error if it does not exist.
# PATCH     - NA (Not Implemented)
# POST      - NA (Not generally implemeneted for an individual resource)
# DELETE    - Delete the addressed member of the collection.
class TaskApi(Resource):
    @marshal_with(task_fields)
    def get(self, **kwargs):
        tid = kwargs["id"]
        abort_if_task_doesnt_exist(tid)
        ta = get_task(tid)
        return ta

    def delete(self, **kwargs):
        tid = kwargs["id"]
        abort_if_task_doesnt_exist(tid)
        ta = get_task(tid)
        remove_task(ta)
        return '', 204

    @marshal_with(task_fields)
    def put(self, **kwargs):
        args = parser.parse_args()
        tid = kwargs["id"]
        abort_if_task_doesnt_exist(tid)
        ta = get_task(tid)
        ta.desc = args['desc']
        return ta, 201

# TaskListApi
# GET       - List the URIs and perhaps other details of the collection's members.
# PUT       - Replace the entire collection with another collection.
# PATCH     - NA (Not generally implemented for a collection)
# POST      - Create a new entry in the collection.
# DELETE    - Delete the entire collection.
class TaskListApi(Resource):
    @marshal_with(task_fields)
    def get(self, **kwargs):
        return get_task_list()

    def put(self, **kwargs):
        args = parser.parse_args()
        tlist = args['tasklist']
        remove_all_tasks()
        for tstr in tlist:
            dict = eval(tstr)
            create_new_task(dict['taskid'], dict['desc'])
        return '', 204

    @marshal_with(task_fields)
    def post(self, **kwargs):
        args = parser.parse_args()
        ta = create_new_task(args['taskid'], args['desc'])
        return ta, 201

    def delete(self, **kwargs):
        remove_all_tasks()
        return '', 204

## Api resource routing
api.add_resource(TaskListApi, '/tasks', endpoint='tasklist_ep')
api.add_resource(TaskApi, '/tasks/<id>', endpoint='task_ep')

## Intialize some Tasks
#add_task(Task(id='task1', desc='The First Task'))
#add_task(Task(id='task2', desc='The Second Task'))
#add_task(Task(id='task3', desc='The Third Task'))

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0')
