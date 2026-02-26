# CLI client 
```
Application name -> cicd-cli

Language -> Java

Build tool -> Gradle

Gradle plugins:
    Test Coverage -> jacoco
    Documentation -> Javadoc
    Style check -> checkstyle
    staticanalysis -> pmd


Libraries:
    Yaml parser library -> snakeYaml
    CLI parser -> Picocli
    Test -> Junit
    Java Core -> Guava

CI/CD

pr:
    compile
    test
    style check
    static analysis

main:
    compile
    test
    style check
    static analysis
    java doc

release
    compile
    test
    style check
    static analysis
    java doc
    jar
    docker push

(to be updated as project goes)
```

# CICD Engine
```
Application name -> cicd-engine

Language -> Java

Git client -> JGit

Docker Client -> Docker Java Client

Build tool -> Gradle

(to be updated as project goes)
```

# Data Storage
```
K8s Client -> Fabric8 Kubernetes Client

(to be updated as project goes)
```