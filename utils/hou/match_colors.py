import math


def lab_dist(a, b):
    d = (a[0] - b[0]) ** 2 + (a[1] - b[1]) ** 2 + (a[2] - b[2]) ** 2
    return math.sqrt(d)


node = hou.pwd()
geo = node.geometry()

palette = hou.pwd().evalParm("palette")

for point in geo.points():
    cd = hou.Color(point.floatListAttribValue("Cd"))
    min_dist = float("inf")
    last_match_color = cd

    for color in palette.values():
        cp = hou.Color(color)
        cd_point = cd.lab()
        cd_palette = cp.lab()

        current_dist = lab_dist(cd_point, cd_palette)

        if min_dist > current_dist:
            min_dist = current_dist
            last_match_color = cp

    point.setAttribValue("Cd", last_match_color.rgb())
