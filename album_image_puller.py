from time import sleep
from requests import get
from requests import post
from string import capwords
import os
import errno
import shutil
import glob
# String, String -> Void
# Doing the initial 
def get_image(album, artist):
    def download_image(path,url):
        if not glob.glob(path +"*"):
            try:
                path += "jpg"
                os.makedirs(os.path.dirname(path))
            except OSError as exc: # Guard against race condition
                if exc.errno != errno.EEXIST:
                    raise
        r = get(url)
        with open(path,'wb') as f:
            f.write(r.content)
    path = f"Homescreen/{artist}/{album}."
    if not glob.glob(path +"*"):
        api = f"https://itunesartwork.bendodson.com/api.php?query={album} {artist}&entity=album&country=us&type=request"
        print(f"Artist: {artist} \nAlbum: {album}")
        r = get(url=api)

        data = r.json()['url']
        r = get(url=data)
        PARAMS = {"json": r.content,
                "type": "data",
                "entity": "album"}

        r = post(url="https://itunesartwork.bendodson.com/api.php",data=PARAMS)

        imageUrl = r.json()[0]["hires"]
        download_image(path,imageUrl)
        print("Sleeping")
        sleep(1)
        print("Done Sleeping")

with open("list.txt","r",encoding="utf8") as f:
    lines = f.readlines()
    artist = ""
    album = ""

    for line in lines:
        line = line.strip()
        if line[0] is "#":
            artist = line[1:]
            album = ""
        else:
            album = line
        if album is not "" and artist is not "":
            get_image(album,artist)
