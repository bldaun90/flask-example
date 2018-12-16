import unittest
import requests
import json

# Verbose:  python test-task-api.py -v
# Testing Reference:  1. HTTP Requests (POST, PUT, etc)  2. Unit Testing
# http://docs.python-requests.org/en/v1.0.0/user/quickstart/#make-a-request
# https://stackoverflow.com/questions/41718376/how-to-unit-test-a-flask-restful-api

BASE_TASKS_URL = "http://127.0.0.1:5000/tasks"
MESSAGE_SUCCESS =  {"message": "success"}
tasks_get1 = [{'datasetid': 'tasktest', 'taskid': 'task1', 'desc': 'The First Task', 'dur': 60, 'uri': 'http://127.0.0.1:5000/tasks/tasktest/task1'}, {'datasetid': 'tasktest', 'taskid': 'task2', 'desc': 'The Second Task', 'dur': 120, 'uri': 'http://127.0.0.1:5000/tasks/tasktest/task2'}, {'datasetid': 'tasktest', 'taskid': 'task3', 'desc': 'The Third Task', 'dur': 30, 'uri': 'http://127.0.0.1:5000/tasks/tasktest/task3'}]
task_get1 = {'datasetid': 'tasktest', 'taskid': 'task3', 'desc': 'The Third Task', 'dur': 30, 'uri': 'http://127.0.0.1:5000/tasks/tasktest/task3'}
tasks_get2 = [{'datasetid': 'tasktest', 'taskid': 'task1', 'desc': 'The First Task', 'dur': 60, 'uri': 'http://127.0.0.1:5000/tasks/tasktest/task1'}, {'datasetid': 'tasktest', 'taskid': 'task2', 'desc': 'The Second Task', 'dur': 120, 'uri': 'http://127.0.0.1:5000/tasks/tasktest/task2'}]
tasks_get3 = [{'datasetid': 'tasktest', 'taskid': 'task1', 'desc': 'The First Task', 'dur': 60, 'uri': 'http://127.0.0.1:5000/tasks/tasktest/task1'}, {'datasetid': 'tasktest', 'taskid': 'task2', 'desc': 'The Second Task', 'dur': 120, 'uri': 'http://127.0.0.1:5000/tasks/tasktest/task2'}, {'datasetid': 'tasktest', 'taskid': 'task3', 'desc': 'The Third Task', 'dur': 30, 'uri': 'http://127.0.0.1:5000/tasks/tasktest/task3'}, {'datasetid': 'tasktest', 'taskid': 'task4', 'desc': 'The Fourth Task', 'dur': 45, 'uri': 'http://127.0.0.1:5000/tasks/tasktest/task4'}]
tasks_get4 = [{'datasetid': 'tasktest', 'taskid': 'task1', 'desc': 'The First Task', 'dur': 60, 'uri': 'http://127.0.0.1:5000/tasks/tasktest/task1'}, {'datasetid': 'tasktest', 'taskid': 'task2', 'desc': 'The Second Task', 'dur': 120, 'uri': 'http://127.0.0.1:5000/tasks/tasktest/task2'}, {'datasetid': 'tasktest', 'taskid': 'task3', 'desc': 'The Third Task Again', 'dur': 33, 'uri': 'http://127.0.0.1:5000/tasks/tasktest/task3'}]

class TestTaskApi(unittest.TestCase):

    def setUp(self):
        # Delete all tasks in test dataset
        url = BASE_TASKS_URL + "/tasktest"
        response = requests.get(url)
        for task_json in response.json():
            url = BASE_TASKS_URL + "/tasktest/" + task_json['taskid']
            response = requests.delete(url)
        # Create 3 tasks in test dataset
        url = BASE_TASKS_URL + "/tasktest/task1"
        payload = {"desc": "The First Task", "dur": 60}
        response = requests.post(url, json=payload)
        url = BASE_TASKS_URL + "/tasktest/task2"
        payload = {"desc": "The Second Task", "dur": 120}
        response = requests.post(url, json=payload)
        url = BASE_TASKS_URL + "/tasktest/task3"
        payload = {"desc": "The Third Task", "dur": 30}
        response = requests.post(url, json=payload)

#    def tearDown(self):
#        print("tearDown")

    #####################################

    def test_get_task_datasets(self):
        tasktest_exists = False
        url = BASE_TASKS_URL
        response = requests.get(url)
        for dataset_json in response.json():
            if (dataset_json['dataset'] == 'tasktest.ta'):
                tasktest_exists = True
        self.assertEqual(tasktest_exists, True)

    def test_get_tasks(self):
        url = BASE_TASKS_URL + "/tasktest"
        response = requests.get(url)
        self.assertEqual(response.json(), tasks_get1)

    def test_get_task(self):
        url = BASE_TASKS_URL + "/tasktest/task3"
        response = requests.get(url)
        self.assertEqual(response.json(), task_get1)

    def test_delete_task(self):
        # delete task
        url = BASE_TASKS_URL + "/tasktest/task3"
        response = requests.delete(url)
        self.assertEqual(response.json(), MESSAGE_SUCCESS)
        # check for non-existence
        url = BASE_TASKS_URL + "/tasktest"
        response = requests.get(url)
        self.assertEqual(response.json(), tasks_get2)

    def test_add_task(self):
        # add task
        url = BASE_TASKS_URL + "/tasktest/task4"
        payload = {"desc": "The Fourth Task", "dur": 45}
        response = requests.post(url, json=payload)
        self.assertEqual(response.json(), MESSAGE_SUCCESS)
        # check for existence
        url = BASE_TASKS_URL + "/tasktest"
        response = requests.get(url)
        self.assertEqual(response.json(), tasks_get3)

    def test_update_task(self):
        # update task
        url = BASE_TASKS_URL + "/tasktest/task3"
        payload = {"desc": "The Third Task Again", "dur": 33}
        response = requests.post(url, json=payload)
        self.assertEqual(response.json(), MESSAGE_SUCCESS)
        # check for existence
        url = BASE_TASKS_URL + "/tasktest"
        response = requests.get(url)
        self.assertEqual(response.json(), tasks_get4)

if __name__ == "__main__":
    unittest.main()
