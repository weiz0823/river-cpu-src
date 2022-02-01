import socket
import _thread as thread          
 
s = socket.socket()         
host = socket.gethostname() 
port = 3456
 
s.connect((host, port))

def receive():
    while True:
        msg = s.recv(1)
        print(bytes.decode(msg), end="")

thread.start_new_thread(receive, ())

while True:
    something = input()
    s.sendall(str.encode(something + '\n'))

s.close()