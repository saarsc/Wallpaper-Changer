#!/usr/bin/env python3
import json
from configparser import ConfigParser

from sys import exc_info

from os.path import exists

from keyrings.alt.file import PlaintextKeyring

import gkeepapi

from flask import Flask,request, jsonify
from collections import OrderedDict


app = Flask(__name__)
app.config['JSON_SORT_KEYS'] = False


# Logins into GKepp to recive the token
def login():
    keep = gkeepapi.Keep()

    creds = ConfigParser()
    creds.read("creds.ini")

    EMAIL = creds["Creds"]["email"]
    PASSWORD = creds["Creds"]["password"]

    key = PlaintextKeyring()
    # In case first login or faulty (old token) use actul login with creds
    def useCreds():
        success = keep.login(EMAIL, PASSWORD,sync=False)
        token = keep.getMasterToken()

        key.set_password("google-keep-token","root",token)

    # Trying to use the stored token
    token = key.get_password("google-keep-token","root")
    if token is not None:
        try:
            keep.resume(EMAIL,token,sync=False)
        except:
            useCreds()
    else:
        useCreds()
    del EMAIL
    del PASSWORD
    del key
    return keep
    # Get all the notes from gKeep
def getNotes(keep):
    # Write current notes to a cache so only deltas would be pulled
    def saveCache():
        with open("notesCache","w") as f:
            state =  keep.dump()
            json.dump(state,f)
    

    if(exists("notesCache")):
        with open("notesCache","r") as f:
            keep.restore(json.load(f))
        keep.sync()
    else:
        keep.sync()
    saveCache()
    return keep.all() 
# Search for the relvent note
# @Return the relevent note
def getPinned(keep):
    notes =  getNotes(keep)

    for note in notes:
        if note.pinned and note.title != "VINYL" and note.color == gkeepapi.node.ColorValue.Red and "round" in note.title.lower():
            return note

def getAllNotes(keep):
    notes = getNotes(keep)
    listData = {}
    for note in notes:
        if note.color == gkeepapi.node.ColorValue.Red and "round" in note.title.lower():
            listData.update({note.title : note.text})
    # endList = {}
    # for i in range(len(listData) -1,0,-1):
    #     key = list(listData.keys())[i]
    #     endList.update({key : listData[key]})
    return  OrderedDict(reversed(list(listData.items())))

def updateNote(line):
    try:
        print(line)
        keep = login()
        note = getPinned(keep)
        
        note.text += f"\n{line}"
        keep.sync()

        return app.response_class(
            response = "Note has been updated",
            status = 200
        )
    except:
        return app.response_class(
            response = "Error updating note",
            status = 500
        )

def createNewNote(roundName):
    try:
        keep = login()
        oldNote = getPinned(keep)
        oldNote.pinned = False

        newNote = keep.createNote(roundName)
        newNote.pinned = True
        newNote.color = gkeepapi.node.ColorValue.Red
        keep.sync()
        
        return app.response_class(
            response = "Note created",
            status = 200
        )
    except:
        return app.response_class(
            response = "Error creating note",
            status = 500
        )

def restore(allRounds):
    # try:
    keep = login()
    if (allRounds):
        return jsonify(getAllNotes(keep)) 
    else:
        note = getPinned(keep).text
        return app.response_class(
            response = note,
            status = 200
        )
    # except:
    #     return app.response_class(
    #         response = "Error with restoring" + str(exc_info()[0]),
    #         status = 500
    #     )

@app.route("/",methods= ["POST"])
def handleAction():
    data=request.get_json()
    action =data["action"]
    if action == "update":
        newLine = data["album"]
        return updateNote(newLine)

    if action=="restore":
        allRound = data["album"]
        return restore(allRound)
        
    if action =="new":
        roundName = data["album"]
        return createNewNote(roundName)
        
    return  app.response_class(
        status = 403
    )
if __name__ == "__main__":
    from waitress import serve
    serve(app, host="0.0.0.0", port=5000)
