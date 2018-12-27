import unittest
import requests
import json

BASE_URL = "https://tidal-nectar-222020.appspot.com/taskdata"
DATASETID = "Test20190101"
DATASET_URL = BASE_URL + "/" + DATASETID
MESSAGE_SUCCESS =  {"message": "success"}
test_get1 = [{'datasetid': 'Test20190101', 'taskid': 'task1', 'desc': 'The 1st Task', 'dur': 11, 'uri': 'https://tidal-nectar-222020.appspot.com/taskdata/Test20190101/task1'}, {'datasetid': 'Test20190101', 'taskid': 'task2', 'desc': 'The 2nd Task', 'dur': 22, 'uri': 'https://tidal-nectar-222020.appspot.com/taskdata/Test20190101/task2'}, {'datasetid': 'Test20190101', 'taskid': 'task3', 'desc': 'The 3rd Task', 'dur': 33, 'uri': 'https://tidal-nectar-222020.appspot.com/taskdata/Test20190101/task3'}]
test_get2 = [{'datasetid': 'Test20190101', 'taskid': 'task97', 'desc': 'The 97th Task', 'dur': 97, 'uri': 'https://tidal-nectar-222020.appspot.com/taskdata/Test20190101/task97'}, {'datasetid': 'Test20190101', 'taskid': 'task98', 'desc': 'The 98th Task', 'dur': 98, 'uri': 'https://tidal-nectar-222020.appspot.com/taskdata/Test20190101/task98'}, {'datasetid': 'Test20190101', 'taskid': 'task99', 'desc': 'The 99th Task', 'dur': 99, 'uri': 'https://tidal-nectar-222020.appspot.com/taskdata/Test20190101/task99'}]
test_get3 = [{'datasetid': 'NewTestDataset2019', 'taskid': 'task1000', 'desc': 'The 1000th Task', 'dur': 1000, 'uri': 'https://tidal-nectar-222020.appspot.com/taskdata/NewTestDataset2019/task1000'}]
test_get4 = {'datasetid': 'Test20190101', 'taskid': 'task1', 'desc': 'The 1st Task', 'dur': 11, 'uri': 'https://tidal-nectar-222020.appspot.com/taskdata/Test20190101/task1'}
test_get5 = {'datasetid': 'Test20190101', 'taskid': 'task1', 'desc': 'Task Number 1', 'dur': 111, 'uri': 'https://tidal-nectar-222020.appspot.com/taskdata/Test20190101/task1'}
test_get6 = {'datasetid': 'Test20190101', 'taskid': 'task19', 'desc': 'Task 19', 'dur': 19, 'uri': 'https://tidal-nectar-222020.appspot.com/taskdata/Test20190101/task19'}
test_get7 = {'datasetid': 'Test20190101', 'taskid': 'task19', 'desc': 'The 19th Task', 'dur': 119, 'uri': 'https://tidal-nectar-222020.appspot.com/taskdata/Test20190101/task19'}
test_get8 = {'datasetid': 'Test20190101', 'taskid': 'task50', 'desc': 'Task 50', 'dur': 50, 'uri': 'https://tidal-nectar-222020.appspot.com/taskdata/Test20190101/task50'}
http_status_not_found = 404


def get_row_key(row):
    return row['taskid']

def get_sorted_tasks(json_in):
    json_out = json_in.copy()
    json_out.sort(key=get_row_key)
    return json_out


class TestTaskApi(unittest.TestCase):

    # Test Setup Strategy
    # Setup is used to create/update a dataset before each test.
    # Teardown method is not needed because the PUT in Setup overwrites existing values.

    def setUp(self):
        # Use Dataset PUT to initialize an entire dataset.  PUT overwrites all values, including tasks.
        url = DATASET_URL
        payload = {"desc": "Test Dataset 01 01 2019", "tasklist": [{"taskid": "task1", "desc": "The 1st Task", "dur": "11"}, {"taskid": "task2", "desc": "The 2nd Task", "dur": "22"}, {"taskid": "task3", "desc": "The 3rd Task", "dur": "33"}]}
        response = requests.put(url, json=payload)

    # def tearDown(self):
    #     print("tearDown")

    #####################################

    def test_get_datasets(self):
        url = BASE_URL
        response = requests.get(url)
        testdataset_exists = False
        for dataset_json in response.json():
            if (dataset_json['datasetid'] == DATASETID):
                testdataset_exists = True
        self.assertEqual(testdataset_exists, True)

    def test_get_dataset_existence(self):
        url = BASE_URL + "/" + "NonExistentDataset"
        response = requests.get(url)
        self.assertEqual(response.status_code, http_status_not_found)

    def test_get_dataset_tasks(self):
        url = DATASET_URL
        response = requests.get(url)
        json = get_sorted_tasks(response.json())
        self.assertEqual(len(json), 3)
        self.assertEqual(json, test_get1)

    def test_update_dataset(self):
        url = DATASET_URL

        # Update the description only - Tasks should NOT be unaffected.
        payload = {"desc": "Change Description Only"}
        response = requests.put(url, json=payload)
        self.assertEqual(response.json(), MESSAGE_SUCCESS)
        #
        response = requests.get(url)
        json = get_sorted_tasks(response.json())
        self.assertEqual(len(json), 3)
        self.assertEqual(json, test_get1)

        # Update the dataset with new tasks.
        url = DATASET_URL
        payload = {"desc": "All New Tasks", "tasklist": [{"taskid": "task99", "desc": "The 99th Task", "dur": "99"}, {"taskid": "task98", "desc": "The 98th Task", "dur": "98"}, {"taskid": "task97", "desc": "The 97th Task", "dur": "97"}]}
        response = requests.put(url, json=payload)
        self.assertEqual(response.json(), MESSAGE_SUCCESS)
        #
        response = requests.get(url)
        json = get_sorted_tasks(response.json())
        self.assertEqual(len(json), 3)
        self.assertEqual(json, test_get2)

    def test_create_delete_dataset(self):
        url = BASE_URL + "/" + "NewTestDataset2019"

        # Check for existence of proposed new dataset
        response = requests.get(url)
        self.assertEqual(response.status_code, http_status_not_found)

        # Create new dataset
        payload = {"desc": "New Test Dataset 2019", "tasklist": [{"taskid": "task1000", "desc": "The 1000th Task", "dur": "1000"}]}
        response = requests.put(url, json=payload)
        self.assertEqual(response.json(), MESSAGE_SUCCESS)
        #
        response = requests.get(url)
        json = get_sorted_tasks(response.json())
        self.assertEqual(len(json), 1)
        self.assertEqual(json, test_get3)

        # Delete the dataset and check for existence
        response = requests.delete(url)
        self.assertEqual(response.json(), MESSAGE_SUCCESS)
        #
        response = requests.get(url)
        self.assertEqual(response.status_code, http_status_not_found)

    def test_get_dataset_task_existence(self):
        url = DATASET_URL + "/taskthatdoesnotexist1"
        response = requests.get(url)
        self.assertEqual(response.status_code, http_status_not_found)

    def test_get_dataset_task(self):
        url = DATASET_URL + "/task1"
        response = requests.get(url)
        self.assertEqual(response.json(), test_get4)

    def test_update_dataset_task(self):

        # update existing task
        url = DATASET_URL + "/task1"
        payload = {"desc": "Task Number 1", "dur": 111}
        response = requests.put(url, json=payload)
        self.assertEqual(response.json(), MESSAGE_SUCCESS)
        #
        response = requests.get(url)
        self.assertEqual(response.json(), test_get5)

        # create a new task and update
        url = DATASET_URL + "/task19"
        payload = {"desc": "Task 19", "dur": 19}
        response = requests.put(url, json=payload)
        self.assertEqual(response.json(), MESSAGE_SUCCESS)
        response = requests.get(url)
        self.assertEqual(response.json(), test_get6)
        #
        payload = {"desc": "The 19th Task", "dur": 119}
        response = requests.put(url, json=payload)
        self.assertEqual(response.json(), MESSAGE_SUCCESS)
        response = requests.get(url)
        self.assertEqual(response.json(), test_get7)

    def test_create_delete_dataset_task(self):
        url = DATASET_URL + "/task50"

        # Check for existence of proposed new task
        response = requests.get(url)
        self.assertEqual(response.status_code, http_status_not_found)

        # Create new task
        payload = {"desc": "Task 50", "dur": 50}
        response = requests.put(url, json=payload)
        self.assertEqual(response.json(), MESSAGE_SUCCESS)
        response = requests.get(url)
        self.assertEqual(response.json(), test_get8)

        # Delete the task and check for existence
        response = requests.delete(url)
        self.assertEqual(response.json(), MESSAGE_SUCCESS)
        response = requests.get(url)
        self.assertEqual(response.status_code, http_status_not_found)


if __name__ == "__main__":
    unittest.main()
