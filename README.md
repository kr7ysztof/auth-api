# Authentication API

The idea is to have an api which authorize a user based on kerberos/ldap ... and return S3 access and secret keys.


Now there are kerberos and radosgw implementations:

Kerberos authorization is based on the [akka-http-spnego](https://github.com/tresata/akka-http-spnego) and the radosgw S3 access/secret keys uses [radosgw-admin4j](https://github.com/twonote/radosgw-admin4j) project.

## Endpionts

### CEPH
**_/ceph/credential/{bucketName}?expirationTimeInMs=7200000_** - get a token for the {bucketName} (the parameter expirationTimeInMs is optional):
```
curl -k --negotiate -u : https://yourserver:12345/ceph/credential/bucketName
```
## Configuration

* Create a keytab for HTTP/yourserver@YOURDOMAIN on the server

* Set environment variables:

```
  AUTH_API_KERBEROS_PRINCIPAL - a http service principal name
  AUTH_API_KERBEROS_KEYTAB - a http service keytab file
  AUTH_API_KERBEROS_DEBUG - true/false debug mode
  AUTH_API_PORT - the api port
  AUTH_API_INTERFACE - the api interface (default 0.0.0.0)
  AUTH_API_HOSTNAME - the api hostname
  AUTH_API_RGW_ACCESS_KEY - the admin radosgw access key 
  AUTH_API_RGW_SECRET_KEY - the admin radosgw secret key
  ?AUTH_API_RGW_ENDPOINT - the radowgw endpoint
```

## Building the project

```bash
sbt clean coverage test coverageReport assembly
```

## Starting the rest api

```bash
java -cp auth-api.jar -Djavax.net.ssl.keyStore=src/main/resources/testKeystore -Djavax.net.ssl.keyStorePassword=changeit nl.wbaa.auth.Server 
```

## Test client

```bash
curl -k --negotiate -u : -b ~/cookiejar.txt -c ~/cookiejar.txt https://yourserver:12345/ceph/credential/bucketName
```


## Swagger documentation

https://yourserver:12345/api-docs/swagger.json


## Implementation

AKKA-HTTP is used to publish the api.
The starting point is

[nl.wbaa.auth.Server](/src/main/scala/nl/wbaa/auth/Server.scala)

