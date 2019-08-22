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

## Code structure
Code is written using Java 8, tests are written with Groovy(Spock), Build Framework - Gradle 


## License
[MIT](https://choosealicense.com/licenses/mit/)
