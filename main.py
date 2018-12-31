"""
REST Task Example using Flask-Restful Library in App Engine / Cloud Datastore.
Tested with Python 3.7

Reference:
  https://cloud.google.com/datastore/docs/concepts/transactions
  https://en.wikipedia.org/wiki/Optimistic_concurrency_control
  https://cloud.google.com/datastore/docs/best-practices

// Profile
curl https://tidal-nectar-222020.appspot.com/profile/enable -X GET
curl https://tidal-nectar-222020.appspot.com/profile/disable -X GET
curl https://tidal-nectar-222020.appspot.com/profile/clear -X GET
curl https://tidal-nectar-222020.appspot.com/profile/report -X GET


// Get datasets (ancestors)
curl https://tidal-nectar-222020.appspot.com/taskdata -X GET


// Get dataset (and tasks)
curl https://tidal-nectar-222020.appspot.com/taskdata/Task20181226 -X GET

// Create or update a dataset - Replaces existing tasks
curl https://tidal-nectar-222020.appspot.com/taskdata/Task20181226 -X PUT -v -H "Content-type: application/json" -d "{\"desc\": \"Dataset 12 26 2018\", \"tasklist\": [{\"taskid\": \"task1\", \"desc\": \"The First Task\", \"dur\": \"11\"}, {\"taskid\": \"task2\", \"desc\": \"The Second Task\", \"dur\": \"22\"}]}"
curl https://tidal-nectar-222020.appspot.com/taskdata/Task20181226 -X PUT -v -H "Content-type: application/json" -d "{\"desc\": \"Dataset Test\", \"tasklist\": [{\"taskid\": \"task8\", \"desc\": \"The 8th Task\", \"dur\": \"8\"}, {\"taskid\": \"task9\", \"desc\": \"The 9th Task\", \"dur\": \"9\"}]}"
curl https://tidal-nectar-222020.appspot.com/taskdata/Task20181226 -X PUT -v -H "Content-type: application/json" -d "{\"desc\": \"Change description only\"}"

// Delete a dataset (ancestor) and its tasks (descendants)
curl https://tidal-nectar-222020.appspot.com/taskdata/Task20181226 -X DELETE


// Get a task
curl https://tidal-nectar-222020.appspot.com/taskdata/Task20181226/task1 -X GET

// Create or Update a task
curl https://tidal-nectar-222020.appspot.com/taskdata/Task20181226/task1 -X PUT -v -H "Content-type: application/json" -d "{\"desc\": \"Task Number 1\", \"dur\": \"11\"}"

// Delete a task
curl https://tidal-nectar-222020.appspot.com/taskdata/Task20181226/task1 -X DELETE


Object Terminology:
   "entity" - An object in Datastore - Equivalent to a database row.
   "new"    - References creating a new in-memory python object.
   "create" - References creating a new Entity in Datastore.
   "delete" - References deleting an Entity from Datastore.
"""

from flask import Flask
from flask_restful import reqparse, abort, Api, Resource, fields, marshal
import json
import os
import datetime
from google.cloud import datastore
import profile

app = Flask(__name__)
api = Api(app)

MESSAGE_SUCCESS = {"message": "success"}

parser = reqparse.RequestParser()
parser.add_argument('desc')
parser.add_argument('dur')
parser.add_argument('tasklist', action='append')


def get_client():
    return datastore.Client()

def get_dataset_key(client, datasetid):
    key = client.key('Dataset', datasetid)
    return key

def get_task_key(client, datasetid, taskid):
    # create a key with Ancestor 'Dataset' key name
    key = client.key('Dataset', datasetid, 'Task', taskid)
    return key

#
# Profile Output
#

clock_fields = {
    'profile_id': fields.String,
    'clock_id': fields.String,
    'elapsed_time': fields.Integer,
    'start_num': fields.Integer,
    'stop_num': fields.Integer
}

class ClockOutput(object):
    def __init__(self, profile_id, clock_id, elapsed_time, start_num, stop_num):
        self.profile_id = profile_id
        self.clock_id = clock_id
        self.elapsed_time = elapsed_time
        self.start_num = start_num
        self.stop_num = stop_num

def get_clocks():
    outlist = []
    clist = profile.get_clocks()
    for pclock in clist:
        clockout = ClockOutput(profile_id=pclock.profile_id, clock_id=pclock.clock_id, elapsed_time=pclock.elapsed_time, start_num=pclock.start_num, stop_num=pclock.stop_num)
        outlist.append(clockout)
    return outlist

#
# DATASET
#

dataset_fields = {
    'datasetid': fields.String,
    'desc': fields.String,
    'uri':  fields.Url('dataset_ep', absolute=True)
}

class Dataset(object):
    def __init__(self, datasetid, desc):
        self.datasetid = datasetid
        self.desc = desc

def new_dataset(datasetid, desc):
    dataset = Dataset(datasetid=datasetid, desc=desc)
    return dataset

def create_dataset(client, key, datasetid, desc, tasklist):
    with client.transaction():
        # First create the dataset ancestor
        entity = datastore.Entity(key, exclude_from_indexes=['datasetid', 'desc'])
        entity.update({
            'created': datetime.datetime.utcnow(),
            'datasetid': datasetid,
            'desc': desc
        })
        client.put(entity)
        # Create new tasks
        if (tasklist):
            for task in tasklist:
                tkey = get_task_key(client, task.datasetid, task.taskid)
                create_task(client, tkey, task.datasetid, task.taskid, task.desc, task.dur)

def update_dataset(client, key, desc, tasklist):
    with client.transaction():
        # first update the dataset object
        entity = client.get(key)
        entity['desc'] = desc
        client.put(entity)
        # replace existing tasks with new tasks
        if (tasklist):
            # delete existing tasks
            query = client.query(kind='Task', ancestor=key)
            for entity in query.fetch():
                client.delete(entity.key)
            # create new tasks
            for task in tasklist:
                tkey = get_task_key(client, task.datasetid, task.taskid)
                create_task(client, tkey, task.datasetid, task.taskid, task.desc, task.dur)

def delete_dataset(client, key):
    with client.transaction():
        # First delete all of the descendant Task entities
        query = client.query(kind='Task', ancestor=key)
        for entity in query.fetch():
            client.delete(entity.key)
        # Then delete the ancestor Dataset entity
        client.delete(key)

def get_datasets(client):
    dlist = []
    query = client.query(kind='Dataset')
    for entity in query.fetch():
        ds = new_dataset(entity['datasetid'], entity['desc'])
        dlist.append(ds)
    return dlist

#
# TASK
#

task_fields = {
    'datasetid': fields.String,
    'taskid': fields.String,
    'desc': fields.String,
    'dur': fields.Integer,
    'uri':  fields.Url('task_ep', absolute=True)
}

# Task is a Descendant of Ancestor Dataset
class Task(object):
    def __init__(self, datasetid, taskid, desc, dur):
        self.datasetid = datasetid
        self.taskid = taskid
        self.desc = desc
        self.dur = dur

def new_task(datasetid, taskid, desc, dur):
    ta = Task(datasetid=datasetid, taskid=taskid, desc=desc, dur=dur)
    return ta

# Does not utilize a transaction because of usage in create and update dataset.
def create_task(client, key, datasetid, taskid, desc, dur):
    entity = datastore.Entity(key, exclude_from_indexes=['taskid', 'desc', 'dur'])
    entity.update({
        'created': datetime.datetime.utcnow(),
        'taskid': taskid,
        'desc': desc,
        'dur': dur
    })
    client.put(entity)
    return entity.key

def update_task(client, key, desc, dur):
    with client.transaction():
        entity = client.get(key)
        entity['desc'] = desc
        entity['dur'] = dur
        client.put(entity)

def delete_task(client, key):
    with client.transaction():
        client.delete(key)

def get_tasks(client, key, datasetid):
    tlist = []
    query = client.query(kind='Task', ancestor=key)
    for entity in query.fetch():
        task = new_task(datasetid, entity['taskid'], entity['desc'], entity['dur'])
        tlist.append(task)
    return tlist

#
# REST API
#

# GET - Get task datasets (ancestors)
class DatasetListApi(Resource):
    def get(self, **kwargs):
        profile.clock_start("GET_Datasets")
        client = get_client()
        datasets = get_datasets(client)
        profile.clock_stop("GET_Datasets")
        return marshal(datasets, dataset_fields), 200

# DatasetApi
# GET - Get dataset details (including tasks)
# PUT - Create or Update a dataset.
# DELETE - Delete dataset (ancestor) and all tasks (Descendants)
class DatasetApi(Resource):
    def get(self, **kwargs):
        profile.clock_start("GET_Dataset")
        # existence check for dataset
        datasetid = kwargs["datasetid"]
        client = get_client()
        key = get_dataset_key(client, datasetid)
        entity = client.get(key)
        if not entity:
            profile.clock_stop("GET_Dataset")
            abort(404, message="Dataset {} does not exist".format(datasetid))

        # get tasks and return
        tasks = get_tasks(client, key, datasetid)
        profile.clock_stop("GET_Dataset")
        return marshal(tasks, task_fields), 200

    def put(self, **kwargs):
        profile.clock_start("PUT_Dataset")
        # get values
        datasetid = kwargs["datasetid"]
        args = parser.parse_args()
        desc = args['desc']
        tasklistarg = args['tasklist']

        # Convert the input JSON to a list of task objects,
        # which is less efficient but MUCH safer.
        tasklist = []
        if (tasklistarg != None):
            for taskstr in tasklistarg:
                dict = eval(taskstr)
                task = new_task(datasetid, dict['taskid'], dict['desc'], dict['dur'])
                tasklist.append(task)

        # existence check for dataset
        client = get_client()
        key = get_dataset_key(client, datasetid)
        entity = client.get(key)
        if (entity):
            # update existing dataset
            update_dataset(client, key, desc, tasklist)
        else:
            # create new dataset
            create_dataset(client, key, datasetid, desc, tasklist)

        profile.clock_stop("PUT_Dataset")
        return MESSAGE_SUCCESS, 200

    def delete(self, **kwargs):
        profile.clock_start("DELETE_Dataset")
        # existence check for dataset
        datasetid = kwargs["datasetid"]
        client = get_client()
        key = get_dataset_key(client, datasetid)
        entity = client.get(key)
        if not entity:
            profile.clock_stop("DELETE_Dataset")
            abort(404, message="Dataset {} does not exist".format(datasetid))

        delete_dataset(client, key)
        profile.clock_stop("DELETE_Dataset")
        return MESSAGE_SUCCESS, 200

# TaskApi
# GET    - Get a task
# PUT   - Create or Update a task
# DELETE - Delete a task
class TaskApi(Resource):
    def get(self, **kwargs):
        profile.clock_start("GET_Task")
        # get values
        datasetid = kwargs["datasetid"]
        taskid = kwargs["taskid"]

        # existence check for dataset
        client = get_client()
        key = get_dataset_key(client, datasetid)
        entity = client.get(key)
        if not entity:
            profile.clock_stop("GET_Task")
            abort(404, message="Dataset {} does not exist".format(datasetid))

        # existence check for task
        key = get_task_key(client, datasetid, taskid)
        entity = client.get(key)
        if (not entity):
            profile.clock_stop("GET_Task")
            abort(404, message="Task {} does not exist".format(taskid))

        # Return task
        task = new_task(datasetid, taskid, entity['desc'], entity['dur'])
        profile.clock_stop("GET_Task")
        return marshal(task, task_fields), 200

    def put(self, **kwargs):
        profile.clock_start("PUT_Task")
        # get values
        args = parser.parse_args()
        desc = args['desc']
        dur = args['dur']
        datasetid = kwargs["datasetid"]
        taskid = kwargs["taskid"]

        # existence check for dataset
        client = get_client()
        key = get_dataset_key(client, datasetid)
        entity = client.get(key)
        if not entity:
            profile.clock_stop("PUT_Task")
            abort(404, message="Dataset {} does not exist".format(datasetid))

        # Update if the task exists, otherwise create a new task
        key = get_task_key(client, datasetid, taskid)
        entity = client.get(key)
        if (entity):
            # Update existing task
            update_task(client, key, desc, dur)
        else:
            # Create new task
            create_task(client, key, datasetid, taskid, desc, dur)

        profile.clock_stop("PUT_Task")
        return MESSAGE_SUCCESS, 200

    def delete(self, **kwargs):
        profile.clock_start("DELETE_Task")
        # get values
        datasetid = kwargs["datasetid"]
        taskid = kwargs["taskid"]

        # existence check for dataset
        client = get_client()
        key = get_dataset_key(client, datasetid)
        entity = client.get(key)
        if not entity:
            profile.clock_stop("DELETE_Task")
            abort(404, message="Dataset {} does not exist".format(datasetid))

        # existence check for task
        key = get_task_key(client, datasetid, taskid)
        entity = client.get(key)
        if (not entity):
            profile.clock_stop("DELETE_Task")
            abort(404, message="Task {} does not exist".format(taskid))

        # Delete task and return
        delete_task(client, key)
        profile.clock_stop("DELETE_Task")
        return MESSAGE_SUCCESS, 200

# GET - General purpose get for Profile object testing - TESTING ONLY!
class ProfileApi(Resource):
    def get(self, **kwargs):
        operation = kwargs["operation"]
        if (operation == "report"):
            return marshal(get_clocks(), clock_fields), 200
        else:
            if (operation == "enable"):
                profile.enable()
            elif (operation == "disable"):
                profile.disable()
            elif (operation == "clear"):
                profile.clear()
            return MESSAGE_SUCCESS, 200

## Api resource routing
api.add_resource(DatasetListApi, '/taskdata', endpoint='datasetlist_ep')
api.add_resource(DatasetApi, '/taskdata/<datasetid>', endpoint='dataset_ep')
api.add_resource(TaskApi, '/taskdata/<datasetid>/<taskid>', endpoint='task_ep')
api.add_resource(ProfileApi, '/profile/<operation>', endpoint='profile_ep')

if __name__ == '__main__':
    app.run(debug=True)
