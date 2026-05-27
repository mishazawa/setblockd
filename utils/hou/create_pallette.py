def pack_rgb_to_float(cd):
  h, s, l = cd.hsl()

  return  l, cd

def sort_by_unique(e):
  return e[0]
spareIn = hou.pwd().evalParm("spare_input0")
palette = hou.node(spareIn)

geo = hou.pwd().geometry()

colors = []

for point in geo.points():
  cd = hou.Color(point.floatListAttribValue("Cd"))
  float_value, rgb_value = pack_rgb_to_float(cd)

  colors.append((float_value, rgb_value))

colors.sort(reverse = False, key=sort_by_unique) 

pins = [x[0] for x in colors]
vals = [x[1].rgb() for x in colors]
interp = [hou.rampBasis.Constant] * len(vals)

# Add code to modify contents of geo.
# Use drop down menu to select examples.
palette.parm("palette").set(hou.Ramp(interp, pins, vals))