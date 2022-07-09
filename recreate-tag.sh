#/usr/bin/bash

git tag -d v0.7.0 
git push origin :v0.7.0  
git tag -a v0.7.0  -m "Release 0.7.0"
git push --tag