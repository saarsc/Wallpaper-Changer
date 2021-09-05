import sys,os

with open("t.txt","w",encoding="utf-8") as f:
    PATH = "Homescreen"
    dirs = []
    for _,d,_ in os.walk(PATH):
        for dir in d:
            dirs.append(dir)
    for dir in dirs:
        f.write(f"#{dir}\n")
        for _,_,files in os.walk(os.path.join(PATH,dir)):
            for file in files:
                f.write(f"\t{file.split('.')[0]}\n")