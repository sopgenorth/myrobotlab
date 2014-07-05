#Importing the Libraries

 
from time import sleep
from org.myrobotlab.service import Speech
from org.myrobotlab.service import Runtime
 
#Starting the required Services
 
serial = Runtime.createAndStart("serial","Serial")
chessgame = Runtime.createAndStart("chessgame", "ChessGame")
log = Runtime.createAndStart("log", "Log")
speech = Runtime.create("speech","Speech")
 
#Configureing Speech Service
 
speech.startService()
speech.setLanguage("en")
speech.speak("Game Begins!")
 
count = 0
 
#Connecting Arduino via Serial
 
if not serial.isConnected():
    serial.connect("COM5")
 
# Adding Listeners
serial.addListener("publishByte", python.name, "input") 
chessgame.addListener("computerMoved", python.name, "voice") 
chessgame.addListener("computerMoved", log.name, "log")
chessgame.addListener("makeMove", serial.name, "write") 
 
#Function taking I/P from Serial and sending the data to Chess Game
 
def input():
 
 
   global count
   newByte = int(serial.readByte())
   #we have reached the end of a new line
   if (newByte == 10) :
     chessMove = ""
     while (newByte != 13):
         newByte = serial.readByte()
         chessMove += chr(newByte)
 
     print chessMove   
     chessgame.move(chessMove)
   part1 = chessMove[0:2]
   part2 = chessMove[2:4]
   feedback = "You Played " + part1 + " to " + part2
   speech.speak(feedback)
   print feedback
 
 
# Function "voice" which decodes the move played by the computer and gives a voice feedback
 
def voice():
 
 incoming = msg_chessgame_computerMoved.data[0]
 
 x = y = z = m = False
 x = incoming.startswith("B")
 y = incoming.startswith("N")
 z = incoming.startswith("Q")
 m = incoming.startswith("R") 
 
 if ( x == True):
  part1 = incoming[1:3]
  part2 = incoming[4:6]
  feedback = "Computer played Bishop from " + part1 + " to " + part2
  speech.speak(feedback)
  print feedback
 
 if ( y == True):
  part1 = incoming[1:3]
  part2 = incoming[4:6]
  feedback = "Computer played Knight from " + part1 + " to " + part2
  speech.speak(feedback)
  print feedback
 
 if ( z == True):
  part1 = incoming[1:3]
  part2 = incoming[4:6]
  feedback = "Computer played Queen from " + part1 + " to " + part2
  speech.speak(feedback)
  print feedback
 
 if ( m == True):
  part1 = incoming[1:3]
  part2 = incoming[4:6]
  feedback = "Computer played Rook from " + part1 + " to " + part2
  speech.speak(feedback)
  print feedback
 
 if ( (m == False) and (y == False) and (x == False) and (z == False) ):
  part1 = incoming[0:2]
  part2 = incoming[3:5]
  feedback = "Computer played Pawn from " + part1 + " to " + part2
  speech.speak(feedback)
  print feedback
