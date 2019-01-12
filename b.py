# Example Python code for reading cloud storage buckets
#
# Reference:
#   https://googleapis.github.io/google-cloud-python/latest/storage/index.html
#   https://googleapis.github.io/google-cloud-python/latest/storage/client.html
#   https://googleapis.github.io/google-cloud-python/latest/storage/blobs.html
#   https://cloud.google.com/appengine/docs/standard/python/googlecloudstorageclient/functions#open
#   https://stackoverflow.com/questions/51179455/issue-with-using-cloudstorage-module-in-python
# print(blob.download_as_string())
# blob.upload_from_string('New contents!')
#
# virtualenv env
# . env/bin/activate
# pip install google.cloud.storage
#
# python b.py --bucket tidal-nectar-222020-datasets --file tasks-1.csv --op "l"
# python b.py --bucket tidal-nectar-222020-datasets --file tasks-1.csv --op "r"
# python b.py --file task4.csv --chunk 1
# python b.py --file task4.csv --chunk 2
#
import argparse
from google.cloud import storage

DELIMITER = ","
NEWLINE = "\n"

def list_blobs(bucketname):
    client = storage.Client()
    bucket = client.get_bucket(bucketname)
    blobs = bucket.list_blobs()
    print("\nBlobs in Bucket: " + bucketname)
    for blob in blobs:
        print(blob.name)

# Must return the current fragment.
def process_chunk(chunk, fragment):
    if ('\n' in chunk):
        lines = chunk.split(NEWLINE)
        numlines = len(lines)
        if (numlines > 0):
            linefragment = fragment
            lastline = lines[numlines-1]
            if ('\n' not in lastline):
                # lastline is a fragment
                fragment = lastline
                numlines -= 1
        # process lines
        index = 0
        while (index < numlines):
            line = lines[index]
            if (len(linefragment) > 0):
                print("linef: " + linefragment + line)
                linefragment = ''
            else:
                print("linew: " + line)
            index += 1
    else:
        fragment += chunk

    return fragment

# Reads a blob file in chunks.
def read_blob_file(bucketname, filename, chunksize):
    # get bucket
    client = storage.Client()
    bucket = client.lookup_bucket(bucketname)
    if (not bucket):
        print("\nBucket does not exist: " + bucketname)
        return

    # get blob
    blob = bucket.get_blob(filename)
    if (not blob):
        print("\nBucket file does not exist: " + bucketname + " / " + filename)
        return

    chunksize -= 1      # adjust for download_as_string using inclusize bytes for end
    chunk = ''
    fragment = ''
    position = 0
    while (True):
        chunk = blob.download_as_string(start=position, end=position + chunksize).decode()
        chunklen = len(chunk)
        if (chunklen < chunksize):
            break
        position += chunksize + 1
        if (position >= blob.size):
            break
        fragment = process_chunk(chunk, fragment)

    # process the last chunk
    process_chunk(chunk, fragment)


# This is a shortcut for small files.
def read_blob_file_old1(bucketname, filename):
    print("\nRead: " + bucketname + " / " + filename)
    client = storage.Client()
    try:
        bucket = client.get_bucket(bucketname)
    except google.cloud.exceptions.NotFound:
        print("Sorry, that bucket does not exist!")
    blob = bucket.get_blob(filename)
    blobbytes = blob.download_as_string()
    blobstr = blobbytes.decode('utf8')
    lines = blobstr.split(NEWLINE)
    for line in lines:
        values = line.split(DELIMITER)
        if (len(values) > 2):
            print("Line:")
            for val in values:
                print("   " + val)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers()

    parser.add_argument('--bucket', default='tidal-nectar-222020-datasets')
    parser.add_argument('--file', default='tasks-1.csv')
    parser.add_argument('--op', default='r')
    parser.add_argument('--chunk', default=100)

    args = parser.parse_args()
    # print("\nB: " + args.bucket + "  F: " + args.file + "  Op: " + args.op + "  ChunkSize: " + str(args.chunk))

    if (args.op == "r"):
        read_blob_file(args.bucket, args.file, int(args.chunk))
    elif (args.op == "l"):
        list_blobs(args.bucket)
    else:
        print("\nUnknown\n")
