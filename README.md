# Diagnosis Muliti-Agent Plan (DxMAP)

DxMAP system for multi agent plans diagnosis.\
The program is written as coding part of my Master Degree thesis as Ben-Gurion University.

## Description:
The program inputs:
1. Multi-agent plan
1. Agent behaviour model (agent conflict & failure) models
1. Failed actions for simulation
1. Diagnosis mode: First Solution or Min Carinality solutions or Min Subset solution.

The program output:
The possible failure diagnosis.

## Prerequisites
Java and Gradle are installed


## Running instructions:
Clone git repository and set you current directory to it.

```
> gradle clean compileJava shadowJar
> java -jar build/libs/multiagent-planning-sat-1.0-SNAPSHOT.jar
```
This will run the program in iterative mode.

### Interactive mode:
Main Class:  il.ac.bgu.ProblemRunner

This will display the folllowing menu:
```
Welcome to SAT problem runner
1) Fail model: no effect, Conflict model: no retries
2) Fail model: delay one step, Conflict model: no retries
3) Fail model: no effect, Conflict model: one retry
4) Fail model: delay one step, Conflict model: one retry
5) Quit
Please select fault and conflict model: 
```



## Code structure
Code is written using Java 8, tests are written with Groovy(Spock), Build Framework - Gradle 


## License
[MIT](https://choosealicense.com/licenses/mit/)
