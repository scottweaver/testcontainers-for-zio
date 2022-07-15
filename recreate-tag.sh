#/usr/bin/bash

git tag -d v0.8.0 
git push origin :v0.8.0  
git tag -a v0.8.0  -m "Release 0.8.0"
git push --tag