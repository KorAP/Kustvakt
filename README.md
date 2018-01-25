![Kustvakt](https://raw.githubusercontent.com/KorAP/Kustvakt/master/misc/kustvakt.png)

Kustvakt is a user and policy management component for KorAP (Diewald et al., 2016). It manages user access to resources (i.e. corpus data) that is typically bound with some licensing schemes. The licensing schemes of the IDS resources provided through KorAP (DeReKo) are very complex involving the access location and purposes (Kupietz & Lüngen, 2014). To manage user access to resources, Kustvakt performs query rewriting with document restrictions (Bański et al., 2014).

Kustvakt acts as a middleware in KorAP binding other components, such as [Koral](https://github.com/KorAP/Koral) a query serializer and [Krill](https://github.com/KorAP/Krill) a search component, together. As KorAP's API provider, it provides services, e.g. searching and retrieving annotation data of a match/hit, that can be used by a client, e.g. [Kalamar](https://github.com/KorAP/Kalamar) (a KorAP web user interface) and [KorapSRU](https://github.com/KorAP/KorapSRU) (the CLARIN FCS endpoint for KorAP).

# Versions
* <b>Kustvakt lite version</b>
  
  provides basic search and match info services without user and policy management.

* <b>Kustvakt full version</b>
  
  provides user and policy management and extended services (e.g. resource and annotation services) in addition to the basic services. This version requires a database (Sqlite is provided) and an LDAP system for user authentication.
  
# Web-services

Web-services including their usage examples are described in the [wiki](https://github.com/KorAP/Kustvakt/wiki).


# Setup


Prerequisites: Jdk 1.7, Git, Maven 3, MySQL (optional).

Clone the latest version of Kustvakt
<pre>
git clone git@github.com:KorAP/Kustvakt.git
</pre>

Since Kustvakt requires Krill and Koral, please install [Krill](https://github.com/KorAP/Krill) and [Koral](https://github.com/KorAP/Koral) in your maven local repository.
Adjust the versions of Krill and Koral in ```Kustvakt/core/pom.xml``` 
according to the versions in 
```Koral/pom.xml```
 and 
 ```Krill/pom.xml```.

Install Kustvakt-core in your maven local repository
<pre>
cd Kustvakt/core
mvn clean install
</pre>

Package Kustvakt full version
<pre>
cd ../full
mvn clean package
</pre>
The jar file is located in the ```target/``` folder.

Package Kustvakt lite version
<pre>
cd ../lite
mvn clean package
</pre>
The jar file is located in the ```target/``` folder.

If there are errors regarding tests, please skip them.
<pre>
mvn clean package -DskipTests=true
</pre>

# Setting kustvakt configuration file

Copy the default Kustvakt configuration file (e.g. ```full/src/main/resources/kustvakt.conf``` or ```lite/src/main/resources/kustvakt-lite.conf```), to the same  folder as the Kustvakt jar files  (```/target```). Please do not change the name of the configuration file.

Set krill.indexDir in the configuration file to the location of your Krill index (relative path). In Kustvakt root directory, there is a sample index, e.g.
<pre>krill.indexDir = ../../sample-index</pre>

Set the location of the LDAP configuration file for Kustvakt full version. The file should contain an admin password to access an LDAP system. Without LDAP, user authentication functions and services cannot be used. However, the authentication mechanism can be extended by implementing other authentication methods e.g. using a database. 


<b>Optional custom configuration</b>

Changing Kustvakt server port and host
<pre>
Server.port = 8089
Server.host = localhost
</pre>

Changing Kustvakt service base URI
<pre>
kustvakt.base.url=/kustvakt/*
</pre>
By default, Kustvakt service base URI refers to /api/*


# Running Kustvakt Server
Requires ```kustvakt.conf``` or ```kustvakt-lite.conf``` in the same folder as the jar file. Otherwise assuming sample-index located in the parent directory of the jar file.

Kustvakt full version requires an LDAP configuration file containing an admin password to access an LDAP system. You can still run Kustvakt full version without an LDAP system, but user authentication functions and services cannot be used. Only services for guest/demo user would be available.

<pre>
cd target/
java -jar target/Kustvakt-[lite/full]-[version].jar    
</pre>


# Futher Setup for Developer

Installing lombok is necessary when working with an IDE. Go to the directory of your lombok.jar, e.g ```~/.m2/repository/org/projectlombok/lombok/1.16.6``` and run
<pre>
java -jar lombok-1.16.6.jar
</pre>

Restart your IDE and clean your project.

Copy ```kustvakt.conf``` or ```kustvakt-lite.conf``` from  ```src/main/resources``` to the ```full/``` or ```lite/``` folder. Then the properties the configuration file can be customized.

In an IDE, you can run ```KustvaktLiteServer``` or ```KustvaktServer``` as a normal Java application.

## Changing Database

The default Sqlite database can be switch to a MySQL database.

Copy ```jdbc.properties``` from ```full/src/main/resources``` to the ```full/``` directory. Do not change the filename.
<pre>
cp full/src/main/resources/jdbc.properties full/
</pre>

Remove or comment the Sqlite Setting.

Uncomment the MySQL Setting and fill in the correct values for the ```jdbc.url```,
 ```jdbc.username```
  and ```jdbc.password```.

The default setting for ```jdbc.schemaPath```
includes test data defined in ```full/src/main/resources/db/insert```
and some user roles defined in ```full/src/main/resources/db/predefined```. You can omit the test data by removing
 ```db.insert```.

Save.

You probably would like to git ignore this file to prevent pushing the database password to github.


Open ```full/src/main/resource/default-config.xml``` and search for the 
Spring bean with id "flyway".

Change the dataSource property to refer to the Spring bean with id "dataSource".
<pre>
&lt;property name="dataSource" ref="dataSource" /&gt;
</pre>

While running ```KustvaktServer``` or ```Kustvakt-full-[version].jar```,
MySQL tables will be created to the specified database from the SQL files in 
```full/src/main/resources/db/new-mysql``` and other paths specified in 
```jdbc.schemaPath```.

# Known issues
Tests are verbose - they do not necessarily imply system errors.


# Publication

Diewald, Nils/Hanl, Michael/Margaretha, Eliza/Bingel, Joachim/Kupietz, Marc/Bański, Piotr/Witt, Andreas (2016):
    KorAP Architecture – Diving in the Deep Sea of Corpus Data. In: Calzolari, Nicoletta/Choukri, Khalid/Declerck, Thierry/Goggi, Sara/Grobelnik, Marko/Maegaard, Bente/Mariani, Joseph/Mazo, Helene/Moreno, Asuncion/Odijk, Jan/Piperidis, Stelios (Hrsg.): Proceedings of the Tenth International Conference on Language Resources and Evaluation (LREC 2016), Portorož, Slovenia. Paris: European Language Resources Association (ELRA), 2016. S. 3586-3591.

Bański, Piotr/Diewald, Nils/Hanl, Michael/Kupietz, Marc/Witt, Andreas (2014):
    Access Control by Query Rewriting. The Case of KorAP. In: Proceedings of the Ninth Conference on International Language Resources and Evaluation (LREC’14). European Language Resources Association (ELRA), 2014. S. 3817-3822.


# References

Kupietz, Marc/Lüngen, Harald (2014): Recent Developments in DeReKo. In: Calzolari, Nicoletta et al. (eds.): Proceedings of the Ninth International Conference on Language Resources and Evaluation (LREC'14). Reykjavik: ELRA, 2378-2385.