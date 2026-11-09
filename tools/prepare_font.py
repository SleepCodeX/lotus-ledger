"""Create Lotus Rounded from the OFL Nunito source. Requires fonttools; not needed for normal builds."""
import sys
from pathlib import Path
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools import subset

source, destination = map(Path, sys.argv[1:3])
font = instantiateVariableFont(TTFont(source), {"wght": 600}, inplace=True)
options = subset.Options()
options.name_IDs = ["*"]
options.name_legacy = True
options.name_languages = ["*"]
subsetter = subset.Subsetter(options=options)
subsetter.populate(unicodes=list(range(32, 256)) + [0x2022, 0x2026, 0x2192, 0x2265, 0x2713, 0x2714, 0x2716])
subsetter.subset(font)
names = {1: "Lotus Rounded", 2: "Regular", 3: "LotusRounded-Regular-1.0", 4: "Lotus Rounded", 6: "LotusRounded-Regular", 16: "Lotus Rounded", 17: "Regular"}
for record in font["name"].names:
    if record.nameID in names:
        record.string = names[record.nameID].encode(record.getEncoding())
destination.parent.mkdir(parents=True, exist_ok=True)
font.save(destination)
print(f"Prepared {destination.name}: {destination.stat().st_size} bytes")
