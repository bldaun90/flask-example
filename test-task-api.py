import unittest
import requests
import json

# Verbose:  python test-task-api.py -v
# Testing Reference:  1. HTTP Requests (POST, PUT, etc)  2. Unit Testing
# http://docs.python-requests.org/en/v1.0.0/user/quickstart/#make-a-request
# https://stackoverflow.com/questions/41718376/how-to-unit-test-a-flask-restful-api

json_tasks_123 = [{"uri": "http://127.0.0.1:5000/tasks/task1", "id": "task1", "desc": "The First Task"}, {"uri": "http://127.0.0.1:5000/tasks/task2", "id": "task2", "desc": "The Second Task"}, {"uri": "http://127.0.0.1:5000/tasks/task3", "id": "task3", "desc": "The Third Task"}]
json_tasks_13 = [{"uri": "http://127.0.0.1:5000/tasks/task1", "id": "task1", "desc": "The First Task"}, {"uri": "http://127.0.0.1:5000/tasks/task3", "id": "task3", "desc": "The Third Task"}]
json_task3 = {"uri": "http://127.0.0.1:5000/tasks/task3", "id": "task3", "desc": "The Third Task"}
json_task3_after_put = {"uri": "http://127.0.0.1:5000/tasks/task3", "id": "task3", "desc": "The 3rd Task"}
json_task4 = {"uri": "http://127.0.0.1:5000/tasks/task4", "id": "task4", "desc": "The Fourth Task"}
json_tasks_1234 = [{"uri": "http://127.0.0.1:5000/tasks/task1", "id": "task1", "desc": "The First Task"}, {"uri": "http://127.0.0.1:5000/tasks/task2", "id": "task2", "desc": "The Second Task"}, {"uri": "http://127.0.0.1:5000/tasks/task3", "id": "task3", "desc": "The Third Task"}, {"uri": "http://127.0.0.1:5000/tasks/task4", "id": "task4", "desc": "The Fourth Task"}]
json_tasks_45 = [{"uri": "http://127.0.0.1:5000/tasks/task4", "id": "task4", "desc": "The Fourth Task"}, {"uri": "http://127.0.0.1:5000/tasks/task5", "id": "task5", "desc": "The Fifth Task"}]
json_no_tasks = []
http_status_bad_request = 400

# Needed for Python2 testing
def remove_unicode(j):
    return json.loads(json.dumps(j))

class TestTaskApi(unittest.TestCase):

    def setUp(self):
        # Use Tasks PUT to initialize task1, task2, task3
        payload = {'tasklist': [{'taskid': 'task1', 'desc': 'The First Task'}, {'taskid': 'task2', 'desc': 'The Second Task'}, {'taskid': 'task3', 'desc': 'The Third Task'}]}
        response = requests.put('http://127.0.0.1:5000/tasks', json=payload)

    def tearDown(self):
        # Use Tasks DELETE to delete all tasks
        response = requests.delete('http://127.0.0.1:5000/tasks')

    #####################################

    def test_tasks_get(self):
        response = requests.get('http://127.0.0.1:5000/tasks')
        self.assertEqual(remove_unicode(response.json()), json_tasks_123)

    def test_task_get(self):
        response = requests.get('http://127.0.0.1:5000/tasks/task3')
        self.assertEqual(remove_unicode(response.json()), json_task3)

    def test_task_delete(self):
        response = requests.delete('http://127.0.0.1:5000/tasks/task2')
        self.assertEqual(response.text, '')
        response = requests.get('http://127.0.0.1:5000/tasks')
        self.assertEqual(remove_unicode(response.json()), json_tasks_13)

    def test_tasks_delete(self):
        response = requests.delete('http://127.0.0.1:5000/tasks')
        self.assertEqual(response.text, '')
        response = requests.get('http://127.0.0.1:5000/tasks')
        self.assertEqual(remove_unicode(response.json()), json_no_tasks)

    def test_task_post(self):
        payload = {'taskid': 'task4', 'desc': 'The Fourth Task'}
        response = requests.post('http://127.0.0.1:5000/tasks', json=payload)
        self.assertEqual(remove_unicode(response.json()), json_task4)
        response = requests.get('http://127.0.0.1:5000/tasks')
        self.assertEqual(remove_unicode(response.json()), json_tasks_1234)
        # Test for Duplicate taskid - Should return Http Status 400
        response = requests.post('http://127.0.0.1:5000/tasks', json=payload)
        self.assertEqual(response.status_code, http_status_bad_request)

    def test_task_put(self):
        payload = {'desc': 'The 3rd Task'}
        response = requests.put('http://127.0.0.1:5000/tasks/task3', data=payload)
        self.assertEqual(remove_unicode(response.json()), json_task3_after_put)

    def test_tasks_put(self):
        payload = {'tasklist': [{'taskid': 'task4', 'desc': 'The Fourth Task'}, {'taskid': 'task5', 'desc': 'The Fifth Task'}]}
        response = requests.put('http://127.0.0.1:5000/tasks', json=payload)
        self.assertEqual(response.text, '')
        response = requests.get('http://127.0.0.1:5000/tasks')
        self.assertEqual(remove_unicode(response.json()), json_tasks_45)

if __name__ == "__main__":
    unittest.main()
