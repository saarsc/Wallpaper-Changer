import os
import re
names = []

for root, dirs, files in os.walk("Homescreen"):
    for file in files:
        names.append(file.split(".")[0].strip())

with open("note_to_be_verified.txt",encoding='utf8') as f:
    data = f.readlines()

fixedData = ""
for line in data:
    line = re.sub(r"[^\)\(\-a-zA-Z\u05D0-\u05EA0-9 \+&,:;'\.=]+","",line)
    try:
        name = line.split(")")[1].split("-")[0].strip()
        if name not in names:
            print(name)
        fixedData+=f"{line}\n" 
    except:
        print(f"error: {line}")
with open("fixed.txt","w",encoding='utf8') as f:
    f.write(fixedData)
