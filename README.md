-------------------------------------------------------------
Docker Container

// Start Here
https://runnable.com/docker/python/dockerize-your-python-application
https://medium.com/@mtngt/docker-flask-a-simple-tutorial-bbcb2f4110b5

// Docker Commands
docker -v                       // docker version
docker ps                       // check for running containers
docker ps -a                    // locate containers to remove
docker kill <CONTAINER ID>      // stop running container
docker images                   // show Docker containers - even if they are not running
docker images -a                // locate the ID of the images you want to remove.
docker rmi <Image ID>           // Remove Docker Image
docker rm <Container ID>        // Remove Docker Container

// Remove - First Delete Containers, then delete Images
docker ps -a
docker rm <Container ID>
docker images -a
docker rmi <Image ID>

// Blow away everything
docker system prune -a

// Build (Must use Guest Network)
docker build -t flask-task-api:latest .

// Run
// -d = detatches from the run (no output)
// -p = specifies the port
docker run -d -p 5000:5000 flask-task-api:latest
docker run -p 5000:5000 flask-task-api:latest

// See Docker Port
docker port <Container ID>


// Original Dockerfile
FROM python:3
ADD d-task-api.py /
RUN pip install flask-restful
CMD [ "python", "./d-task-api.py" ]
-------------------------------------------------------------
