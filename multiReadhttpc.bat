cd httpclient
for /L %%G IN (1, 1, 25) DO start /B httpc.exe get http://localhost/codefile.java -v
cd ..