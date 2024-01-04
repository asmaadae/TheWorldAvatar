# Install the stdc app
#==================================================================================================
# Reference published Docker image for Stack-Client resources to use
FROM ghcr.io/cambridge-cares/stack-client:1.20.1 as stackclients


FROM python:3.9.14-slim-buster as base
# Keeps Python from generating .pyc files in the container
ENV PYTHONDONTWRITEBYTECODE=1

# Turns off buffering for easier container logging
ENV PYTHONUNBUFFERED=1

# Install Java
RUN apt update && apt install -y openjdk-11-jdk-headless git

# Copy Stack-Client resource from published Docker image
COPY --from=stackclients /app ./tmp_stack

# Set the default shell
SHELL ["/bin/bash", "-c"]
RUN python -m pip install --upgrade pip
RUN python -m pip install gunicorn

# Set the default working directory, then copy the Python source code into it
WORKDIR /app

COPY ./requirements.txt /app/.
# Install the required Python libraries
RUN python -m pip install -r requirements.txt

# Update py4jps resources
RUN stack_clients_jar=$(find ../tmp_stack/stack-clients*.jar) && stack_clients_jar=${stack_clients_jar##*/} && \
    jpsrm install StackClients ../tmp_stack --jar $stack_clients_jar

COPY ./agent /app/agent
COPY ./app_entry_point.sh /app/.
COPY ./app.py /app/.
COPY ./data /app/data

# Expose port
EXPOSE 5000
ENV FLASK_APP=app.py

FROM base as debug
RUN pip install debugpy --trusted-host pypi.org --trusted-host files.pythonhosted.org

WORKDIR /app
CMD python -m debugpy --listen 0.0.0.0:5678 --wait-for-client -m flask run -h 0.0.0.0 -p 5000

FROM base as production
#------------------------------------
# entry point setup
#------------------------------------
# Set the entrypoint
CMD ["gunicorn", "--bind", "0.0.0.0:5000", "app:app", "--preload", "--timeout", "1800"]
#==================================================================================================