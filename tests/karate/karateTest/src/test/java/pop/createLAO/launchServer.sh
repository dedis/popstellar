#Params for GO
DIR="/mnt/c/Users/Mohamed/GolandProjects/student_21_pop/be1-go"
pk="g6XxoDTcz2tQZLjiK6zK24foSLSxU5P5tUYlKqhedCo="

#Time to wait before shutting down server
timeout=8 

#Make the pop 
make -C $DIR pop 
if [ $? -ne 0  ] && [$? -nq 1];
then 
  exit 1;
fi

#Launch server
$DIR/pop organizer --pk $pk serve >> create.log &

#Terminate Server after timeout
pid=` jobs -p`
sleep $timeout
kill -n SIGTERM $pid
