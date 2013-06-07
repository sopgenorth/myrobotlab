twitter = Runtime.createAndStart("twitter","Twitter")
opencv = Runtime.createAndStart("opencv","OpenCV")

# add a filter to display
# opencv must be a running service, with a camera capturing!!!!
opencv.addFilter("PyramidDown")
opencv.capture()

# replace security information with your own keys : 
# register your application at https://dev.twitter.com/ and obtain your own keys
twitter.setSecurity("yourConsumerKey","yourConsumerSecret", "yourAccessToken", "yourAccessTokenSecret")
twitter.configure()

# sleep - let camera initialize and
# video processor threads start etc... say CHEESE !
sleep(5)

twitter.uploadImage(opencv.getDisplay() , "text to upload");