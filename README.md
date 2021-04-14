Example Voting App
=========

Getting started
---------------

Watch the following video (in German): [Einführung in OpenShift anhand einer Voting-App](https://youtu.be/npnKpuwoWo0) 

Architecture
-----

![Architecture diagram](architecture.png)

* A Python webapp which lets you vote between two options
* A Redis queue which collects new votes
* A Kotlin transporter which consumes votes and stores them in…
* A Postgres database backed by a Docker volume
* An AngularJS webapp which shows the results of the voting in real time