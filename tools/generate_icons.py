from PIL import Image, ImageDraw
import math
N=32
C=(255,0,255)          # Wegpunktfarbe
OUT=(26,26,26)
def col(h): return ((h>>16)&255,(h>>8)&255,h&255)
RED=col(0xD63A2E); DRED=col(0x9E2A20); WOOD=col(0xC89A5C); DWOOD=col(0x8B5A2B); DDWOOD=col(0x5C3A1E)
WHITE=col(0xF2F2F2); LGRAY=col(0xC8C8C8); GRAY=col(0x9A9A9A); DGRAY=col(0x5E5E5E); BLUE=col(0x5DADE2); DBLUE=col(0x2E6DB4)
PINK=col(0xF4A6B0); DPINK=col(0xD9798A); YEL=col(0xF4D03F); DYEL=col(0xC9A227); LYEL=col(0xFBEEA0)
GREEN=col(0x4CAF50); DGREEN=col(0x2E7D32); LGREEN=col(0x8BD17C); CYAN=col(0x4FD1D9); DCYAN=col(0x2A9DA5)
PURP=col(0x9B59B6); DPURP=col(0x5B2C6F); LPURP=col(0xD2B4DE); ORANGE=col(0xF39C12); DORANGE=col(0xD35400)
SKIN=col(0xE8B98A); SKIN2=col(0xC98E5E); SKIN3=col(0x8D5524); HAIR=col(0x5A3A1E); BLACK=col(0x222222)
OBS=col(0x2B1F3A); EMER=col(0x2ECC71); DEMER=col(0x1E8449); BROWN=col(0x7B4A2A); CREEP=col(0x5DBB4C); ZOMB=col(0x5E9E57)

SS=8
TRANSP=(0,0,0,0)
ICONS=[]
def icon(id,label):
    def deco(f):
        im=Image.new("RGBA",(N*SS,N*SS),(0,0,0,0)); d=ImageDraw.Draw(im); f(d); ICONS.append((id,label,down(im))); return f
    return deco
def s_(v): return (v+0.5)*SS
def P(d,pts,c): d.polygon([(s_(x),s_(y)) for x,y in pts],fill=c)
def R(d,x0,y0,x1,y1,c): d.rectangle([x0*SS,y0*SS,(x1+1)*SS-1,(y1+1)*SS-1],fill=c)
def E(d,x0,y0,x1,y1,c): d.ellipse([x0*SS,y0*SS,(x1+1)*SS-1,(y1+1)*SS-1],fill=c)
def L(d,pts,c,w):
    pts=[(s_(x),s_(y)) for x,y in pts]
    d.line(pts,fill=c,width=int(w*SS),joint="curve")
    r=w*SS/2
    for x,y in (pts[0],pts[-1]): d.ellipse([x-r,y-r,x+r,y+r],fill=c)
def ARC(d,box,a0,a1,c,w):
    x0,y0,x1,y1=box; d.arc([x0*SS,y0*SS,(x1+1)*SS-1,(y1+1)*SS-1],a0,a1,fill=c,width=int(w*SS))
def PIE(d,box,a0,a1,c):
    x0,y0,x1,y1=box; d.pieslice([x0*SS,y0*SS,(x1+1)*SS-1,(y1+1)*SS-1],a0,a1,fill=c)
def F(d,fn,c):
    """Implizite Form: fn(x, y) <= 0 in Pixelkoordinaten (0..32)."""
    px=d.im if False else None
    img=d._image; p=img.load()
    for yy in range(N*SS):
        for xx in range(N*SS):
            if fn((xx+0.5)/SS,(yy+0.5)/SS)<=0: p[xx,yy]=c+(255,)
from collections import Counter
def down(im):
    src=im.load(); out=Image.new("RGBA",(N,N),(0,0,0,0)); o=out.load()
    for y in range(N):
        for x in range(N):
            cnt=Counter()
            for dy in range(SS):
                for dx in range(SS):
                    v=src[x*SS+dx,y*SS+dy]
                    cnt[v[:3] if v[3] else None]+=1
            transp=cnt.pop(None,0)
            if transp*2>=SS*SS or not cnt: continue
            o[x,y]=cnt.most_common(1)[0][0]+(255,)
    return out

@icon("house","Zuhause")
def _(d): P(d,[(3,15),(16,3),(28,15)],RED); R(d,6,15,25,28,WOOD); R(d,9,18,14,23,BLUE); R(d,18,19,22,28,DDWOOD)
@icon("spawn","Spawn")
def _(d):
    E(d,3,24,28,30,GRAY); E(d,6,25,25,29,LGRAY)
    E(d,11,2,20,11,C)
    E(d,8,12,23,21,C); P(d,[(8,16),(23,16),(20,26),(11,26)],C)
    for a in (-1,1):
        L(d,[(16+a*8,6),(16+a*11,3)],YEL,2)
        L(d,[(16+a*9,13),(16+a*13,13)],YEL,2)
@icon("bed","Bett")
def _(d): R(d,3,9,6,27,DWOOD); R(d,25,16,28,27,DWOOD); R(d,6,18,25,23,WOOD); R(d,7,14,12,18,WHITE); R(d,13,13,27,18,RED)
@icon("pig","Tiere")
def _(d): R(d,5,7,26,26,PINK); P(d,[(5,7),(5,3),(10,7)],DPINK); P(d,[(26,7),(26,3),(21,7)],DPINK); R(d,11,16,20,22,DPINK); R(d,13,18,14,20,BLACK); R(d,17,18,18,20,BLACK); R(d,9,11,10,13,BLACK); R(d,21,11,22,13,BLACK)
@icon("heart","Herz")
def _(d):
    F(d,lambda X,Y:((((X-16)/11.5)**2+((16.5-Y)/11.5)**2-1)**3-((X-16)/11.5)**2*((16.5-Y)/11.5)**3),RED)
    E(d,7,8,9,10,WHITE)
@icon("friends","Freunde")
def _(d):
    R(d,1,2,13,14,OUT); R(d,2,3,12,13,SKIN2); R(d,2,3,12,6,YEL); R(d,4,8,5,9,DGREEN); R(d,9,8,10,9,DGREEN)
    R(d,18,2,30,14,OUT); R(d,19,3,29,13,SKIN3); R(d,19,3,29,6,BLACK); R(d,21,8,22,9,BLACK); R(d,26,8,27,9,BLACK)
    R(d,9,12,22,27,OUT); R(d,10,13,21,26,SKIN); R(d,10,13,21,17,HAIR); R(d,12,20,13,21,DBLUE); R(d,18,20,19,21,DBLUE); R(d,13,24,18,24,BROWN)
@icon("star","Stern")
def _(d):
    pts=[]
    for i in range(10):
        r=14 if i%2==0 else 6; a=-math.pi/2+i*math.pi/5
        pts.append((15.5+r*math.cos(a),16+r*math.sin(a)))
    P(d,pts,YEL)
@icon("flag","Ziel")
def _(d): R(d,5,3,7,29,GRAY); P(d,[(8,4),(27,4),(22,10),(27,16),(8,16)],C)
@icon("chest","Lager")
def _(d): R(d,3,7,28,26,DWOOD); R(d,3,14,28,15,DDWOOD); R(d,14,12,17,18,LGRAY)
@icon("pickaxe","Mine")
def _(d): L(d,[(9,23),(23,9)],DWOOD,3); ARC(d,(5,4,27,26),200,340,CYAN,4)
@icon("diamond","Erze")
def _(d): P(d,[(9,6),(23,6),(28,12),(16,27),(4,12)],CYAN); P(d,[(4,12),(28,12),(16,27)],DCYAN); R(d,10,8,12,9,WHITE)
@icon("sword","Gefahr")
def _(d): L(d,[(10,22),(26,6)],LGRAY,4); L(d,[(6,18),(14,26)],DYEL,3); L(d,[(5,27),(9,23)],DWOOD,3)
@icon("skull","Todespunkt")
def _(d): E(d,5,3,26,22,WHITE); R(d,10,18,21,27,WHITE); E(d,9,10,14,15,BLACK); E(d,17,10,22,15,BLACK); R(d,13,23,13,27,GRAY); R(d,18,23,18,27,GRAY)
@icon("netherportal","Netherportal")
def _(d): R(d,5,2,26,29,OBS); R(d,9,6,22,25,PURP); L(d,[(10,10),(21,21)],LPURP,2); L(d,[(10,19),(15,24)],LPURP,2)
@icon("endportal","End-Portal")
def _(d): R(d,2,8,29,23,col(0xD9D59C)); R(d,6,11,25,20,BLACK); E(d,12,12,19,19,col(0x2F8F6A)); R(d,15,14,16,17,BLACK)
@icon("bell","Dorf")
def _(d): P(d,[(10,8),(21,8),(25,23),(6,23)],YEL); E(d,10,4,21,13,YEL); R(d,4,23,27,25,DYEL); R(d,14,26,17,28,DYEL)
@icon("wheat","Farm")
def _(d):
    for x in (8,16,24):
        L(d,[(x,14),(x,29)],GREEN,2); E(d,x-3,3,x+3,15,YEL)
@icon("tree","Wald")
def _(d): R(d,14,18,17,29,DWOOD); E(d,4,3,27,22,GREEN); E(d,8,6,14,11,LGREEN)
@icon("cave","Höhle")
def _(d): P(d,[(2,28),(8,8),(16,3),(24,8),(29,28)],GRAY); E(d,9,13,22,34,BLACK)
@icon("mountain","Berg")
def _(d): P(d,[(1,27),(12,6),(20,18),(23,13),(30,27)],GRAY); P(d,[(9,11),(12,6),(15,11)],WHITE)
@icon("water","See / Meer")
def _(d):
    for y in (8,16,24):
        for x in range(2,30,8): ARC(d,(x,y-3,x+8,y+5),180,360,BLUE,3)
@icon("lava","Lava")
def _(d): P(d,[(16,2),(25,17),(7,17)],ORANGE); E(d,7,10,25,28,ORANGE); E(d,11,15,17,21,YEL)
@icon("boat","Hafen")
def _(d): P(d,[(2,19),(29,19),(24,27),(7,27)],DWOOD); R(d,15,4,16,19,DDWOOD); P(d,[(17,5),(26,16),(17,16)],WHITE)
@icon("tower","Turm")
def _(d): R(d,9,9,22,29,GRAY); R(d,7,4,24,9,GRAY); R(d,9,3,11,4,GRAY); R(d,14,3,17,4,GRAY); R(d,20,3,22,4,GRAY); R(d,14,22,17,29,DDWOOD); R(d,14,13,17,16,BLACK)
@icon("castle","Burg")
def _(d): R(d,3,12,28,29,GRAY); R(d,3,5,9,12,GRAY); R(d,22,5,28,12,GRAY); R(d,12,8,19,12,GRAY); E(d,12,19,19,26,DDWOOD); R(d,12,23,19,29,DDWOOD); R(d,15,1,15,8,DGRAY); P(d,[(16,1),(21,3),(16,5)],C)
@icon("tent","Zeltlager")
def _(d): P(d,[(16,4),(29,27),(2,27)],C); P(d,[(16,12),(21,27),(11,27)],BLACK)
@icon("campfire","Lagerfeuer")
def _(d): L(d,[(4,27),(27,22)],DWOOD,3); L(d,[(4,22),(27,27)],DWOOD,3); P(d,[(16,3),(24,17),(20,23),(12,23),(8,17)],ORANGE); P(d,[(16,11),(20,19),(12,19)],YEL)
def rot_ellipse(cx,cy,rx,ry,deg):
    c,sn=math.cos(math.radians(deg)),math.sin(math.radians(deg))
    def f(X,Y):
        dx,dy=X-cx,Y-cy; u=dx*c+dy*sn; v=-dx*sn+dy*c
        return (u/rx)**2+(v/ry)**2-1
    return f
@icon("axe","Holzfäller")
def _(d):
    PIE(d,(8,0,30,22),-20,80,LGRAY)
    L(d,[(7,29),(20,9)],DWOOD,3)
@icon("shovel","Graben")
def _(d):
    L(d,[(6,26),(19,13)],DWOOD,3)
    F(d,rot_ellipse(23,9,4.5,7,45),LGRAY)
@icon("bow","Jagd")
def _(d): ARC(d,(2,2,30,30),-45,135,DWOOD,3); L(d,[(7,25),(25,7)],LGRAY,1); L(d,[(6,26),(27,5)],WHITE,1)
@icon("shield","Basis")
def _(d): P(d,[(5,4),(26,4),(26,16),(16,29),(5,16)],C); L(d,[(16,6),(16,25)],WHITE,2); L(d,[(7,13),(24,13)],WHITE,2)
@icon("potion","Brauerei")
def _(d): R(d,13,3,18,10,LGRAY); E(d,5,9,26,29,LGRAY); E(d,7,14,24,28,C); R(d,12,2,19,4,DWOOD)
@icon("book","Bibliothek")
def _(d): R(d,5,4,26,28,DRED); R(d,8,4,26,25,RED); R(d,8,25,26,28,WHITE); R(d,13,10,22,12,DYEL)
@icon("anvil","Schmiede")
def _(d): P(d,[(3,7),(28,7),(28,12),(20,14),(11,14),(3,10)],DGRAY); R(d,12,14,19,22,DGRAY); R(d,7,22,24,27,DGRAY)
@icon("furnace","Ofen")
def _(d): R(d,4,4,27,28,GRAY); R(d,9,15,22,24,BLACK); R(d,11,19,20,24,ORANGE); R(d,9,8,22,10,DGRAY)
@icon("crafting","Werkbank")
def _(d): R(d,3,5,28,12,col(0xA0522D)); R(d,5,12,26,28,WOOD); R(d,8,15,13,20,DWOOD); R(d,18,15,23,20,LGRAY); R(d,3,7,28,8,DWOOD)
@icon("key","Schlüssel")
def _(d): E(d,3,3,15,15,YEL); E(d,7,7,11,11,(0,0,0,0)); L(d,[(13,13),(27,27)],YEL,3); L(d,[(21,21),(17,25)],YEL,3); L(d,[(25,25),(21,29)],YEL,3)
@icon("crown","Schatz")
def _(d): P(d,[(3,9),(10,17),(16,5),(22,17),(28,9),(26,25),(5,25)],YEL); E(d,14,17,17,20,RED)
@icon("emerald","Handel")
def _(d): P(d,[(16,2),(26,10),(26,22),(16,30),(6,22),(6,10)],EMER); P(d,[(16,8),(21,12),(21,20),(16,24),(11,20),(11,12)],DEMER)
@icon("bucket","Eimer")
def _(d): P(d,[(5,10),(26,10),(23,28),(8,28)],LGRAY); R(d,6,10,25,14,BLUE); ARC(d,(6,1,25,20),180,360,GRAY,2)
@icon("fish","Angeln")
def _(d): E(d,3,9,23,23,BLUE); P(d,[(20,16),(29,8),(29,24)],BLUE); R(d,7,13,9,15,BLACK)
@icon("map","Karte")
def _(d): R(d,3,4,28,27,col(0xF3E5AB)); L(d,[(6,22),(12,14),(18,18),(25,9)],RED,2); L(d,[(22,20),(26,24)],RED,2); L(d,[(26,20),(22,24)],RED,2)
@icon("compass","Kompass")
def _(d): E(d,3,3,28,28,LGRAY); E(d,6,6,25,25,WHITE); P(d,[(16,6),(19,16),(13,16)],RED); P(d,[(16,25),(19,16),(13,16)],DGRAY)
@icon("lantern","Laterne")
def _(d): R(d,13,2,18,5,DGRAY); R(d,8,6,23,9,DGRAY); R(d,9,9,22,25,ORANGE); R(d,8,25,23,28,DGRAY); R(d,12,12,19,22,YEL)
@icon("moon","Mond")
def _(d): E(d,4,4,27,27,LYEL); E(d,11,1,32,22,(0,0,0,0))
@icon("sun","Sonne")
def _(d):
    for i in range(8):
        a=i*math.pi/4; L(d,[(16+8*math.cos(a),16+8*math.sin(a)),(16+14*math.cos(a),16+14*math.sin(a))],ORANGE,3)
    E(d,9,9,23,23,YEL)
@icon("snow","Schnee")
def _(d):
    for i in range(3):
        a=i*math.pi/3; L(d,[(16-13*math.cos(a),16-13*math.sin(a)),(16+13*math.cos(a),16+13*math.sin(a))],col(0xAEE3F5),3)
@icon("flower","Blume")
def _(d):
    L(d,[(16,16),(16,29)],GREEN,2); E(d,18,20,25,25,GREEN)
    for dx,dy in ((0,-6),(6,0),(0,6),(-6,0)): E(d,12+dx,8+dy,20+dx,16+dy,C)
    E(d,13,9,19,15,YEL)
@icon("mushroom","Pilz")
def _(d): R(d,12,16,19,28,WHITE); PIE(d,(3,4,28,28),180,360,RED); E(d,8,8,11,11,WHITE); E(d,19,7,23,11,WHITE); E(d,14,11,17,14,WHITE)
@icon("cactus","Wüste")
def _(d): R(d,12,3,19,29,DGREEN); R(d,4,9,9,17,DGREEN); R(d,4,17,12,20,DGREEN); R(d,22,6,27,13,DGREEN); R(d,19,13,27,16,DGREEN); R(d,3,29,28,29,YEL)
@icon("pumpkin","Kürbis")
def _(d): E(d,3,7,28,28,ORANGE); R(d,14,3,17,8,DGREEN); L(d,[(16,9),(16,27)],DORANGE,2); L(d,[(9,10),(9,26)],DORANGE,1); L(d,[(23,10),(23,26)],DORANGE,1)
@icon("apple","Essen")
def _(d): E(d,4,8,27,28,RED); L(d,[(16,9),(17,3)],DWOOD,2); E(d,18,2,26,7,GREEN); E(d,8,12,11,15,WHITE)
@icon("spawner","Spawner")
def _(d):
    R(d,4,4,27,27,BLACK)
    for x in (4,11,18,25): R(d,x,4,x+2,27,DGRAY)
    for y in (4,11,18,25): R(d,4,y,27,y+2,DGRAY)
    E(d,12,12,19,19,ORANGE)
@icon("zombie","Zombie")
def _(d): R(d,4,4,27,27,ZOMB); R(d,4,4,27,9,col(0x3B6E35)); R(d,8,14,13,17,BLACK); R(d,18,14,23,17,BLACK); R(d,11,21,20,23,col(0x3B6E35))
@icon("creeper","Creeper")
def _(d): R(d,4,4,27,27,CREEP); R(d,8,9,13,14,BLACK); R(d,18,9,23,14,BLACK); R(d,13,14,18,22,BLACK); R(d,10,18,13,25,BLACK); R(d,18,18,21,25,BLACK)
@icon("redstone","Redstone")
def _(d): P(d,[(19,2),(7,18),(15,18),(11,30),(25,12),(17,12),(22,2)],RED)
@icon("cow","Kühe")
def _(d): R(d,5,6,26,26,BROWN); R(d,9,18,22,26,WHITE); R(d,5,6,13,12,WHITE); R(d,9,12,11,14,BLACK); R(d,20,12,22,14,BLACK); R(d,12,21,13,23,BLACK); R(d,18,21,19,23,BLACK); R(d,2,7,5,9,LGRAY); R(d,26,7,29,9,LGRAY)
@icon("egg","Hühner")
def _(d): E(d,7,3,24,29,col(0xF5E6C8)); E(d,10,8,13,13,WHITE)
@icon("circle","Kreis")
def _(d): E(d,4,4,27,27,C); E(d,11,11,20,20,(0,0,0,0))
@icon("triangle","Dreieck")
def _(d): P(d,[(16,3),(29,27),(2,27)],C)
@icon("cross","X")
def _(d): L(d,[(5,5),(26,26)],C,6); L(d,[(5,26),(26,5)],C,6)
@icon("arrow","Pfeil")
def _(d): P(d,[(16,2),(29,15),(21,15),(21,29),(10,29),(10,15),(2,15)],C)
@icon("exclamation","Achtung")
def _(d): P(d,[(16,2),(30,28),(1,28)],YEL); R(d,14,10,17,20,BLACK); R(d,14,23,17,25,BLACK)
@icon("check","Erledigt")
def _(d): P(d,[(2,16),(6,12),(12,18),(25,5),(29,9),(12,26)],GREEN)
@icon("question","Unbekannt")
def _(d): E(d,2,2,29,29,C); ARC(d,(10,6,21,17),180,90,WHITE,3); R(d,14,16,17,20,WHITE); R(d,14,23,17,26,WHITE)

def outline(im):
    px=im.load(); out=im.copy(); op=out.load()
    for y in range(N):
        for x in range(N):
            if px[x,y][3]: continue
            hit=False
            for dx in (-1,0,1):
                for dy in (-1,0,1):
                    xx,yy=x+dx,y+dy
                    if 0<=xx<N and 0<=yy<N and px[xx,yy][3]: hit=True
            if hit: op[x,y]=OUT+(255,)
    return out
ICONS=[(i,l,outline(im)) for i,l,im in ICONS]
print(len(ICONS))
import os, sys
COLS=8
ROWS=(len(ICONS)+COLS-1)//COLS
out_dir=sys.argv[1] if len(sys.argv)>1 else "."
base=Image.new("RGBA",(COLS*N,ROWS*N),(0,0,0,0))
tint=Image.new("RGBA",(COLS*N,ROWS*N),(0,0,0,0))
preview=Image.new("RGBA",(COLS*N,ROWS*N),(45,45,45,255))
lines=[f"atlas {COLS*N} {ROWS*N}"]
for k,(i,l,im) in enumerate(ICONS):
    ox,oy=(k%COLS)*N,(k//COLS)*N
    p=im.load(); hast=False
    for y in range(N):
        for x in range(N):
            r,g,b,a=p[x,y]
            if not a: continue
            if (r,g,b)==C:
                tint.putpixel((ox+x,oy+y),(255,255,255,255)); preview.putpixel((ox+x,oy+y),(60,150,255,255)); hast=True
            else:
                base.putpixel((ox+x,oy+y),(r,g,b,255)); preview.putpixel((ox+x,oy+y),(r,g,b,255))
    lines.append(f"icon {i} {'1' if hast else '0'} {l}")
tex=os.path.join(out_dir,"textures","gui"); os.makedirs(tex,exist_ok=True)
base.save(os.path.join(tex,"icons.png")); tint.save(os.path.join(tex,"icons_tint.png"))
open(os.path.join(out_dir,"icons.txt"),"w").write("\n".join(lines)+"\n")
preview.resize((COLS*N*4,ROWS*N*4),Image.NEAREST).save("sheet.png")
