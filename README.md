# student20_pop
Proof-of-personhood, fall 2020

# overview
We defined the basic structure that mainly follow the publish subscribe model

# Folders

## Database 
The idea is to store a big string which contains all the information (basically in json format)
- Userdb 
   - store and retrieve user from database
   - parse user string(in class jsonHelper
- Channeldb:    
   - store and retrieve channel from database
   - parse chanel string(in class jsonHelper

## Src 
- Hub: The bandmaster of our backend
   - Serve() which basically will be the huge loop needed to run the server and everything
   - OpenConnection() 
   - ListenIncomingMsg()
   - Broadcast() 
   - SendMsg() 
- Subscriber:   
   - Publish data in database
   - Fetch from the server to get the updates of the channels
   - InterpretMessage which will decode string and  call the action method
   - ActAccordingly which will call the function needed from the user concerned 
- Publisher:    
   - Listen to the websocket 
   - Send message to hub

