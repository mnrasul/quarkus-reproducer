

There's an issue where RuleUnit is not injected in the query and things aren't working. 

Build locally

``` 
or mvn clean test -Dquarkus.test.profile.tags=local
quarkus build --native -Dquarkus.test.profile.tags=local
```