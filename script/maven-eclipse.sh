#!/bin/bash

cd `dirname $0`
cd ..

mvn -e eclipse:eclipse

sh dbflute_maihamadb/manage.sh refresh
