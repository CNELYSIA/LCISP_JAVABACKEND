import json
import os
import ee
import geemap
ee.Initialize()


with open(r'config.json', encoding="utf-8") as f:
    config = json.load(f)


Sensor = ee.ImageCollection(config['Option']['Sensor'])
GeoJSON = geemap.geojson_to_ee(config['Geojson'])
roi = GeoJSON.geometry()

collection = Sensor.filterDate(config['Option']['StartTime'], config['Option']['EndTime']) \
    .filter(ee.Filter.lt(config['Option']['Filter'][0], int(config['Option']['Filter'][1]))) \
    .filterBounds(GeoJSON) \
    .select(config['Option']['Bands'])

composite = collection.median().clip(roi)

work_dir = os.path.join(os.path.expanduser("."), 'tif')
if not os.path.exists(work_dir):
    os.makedirs(work_dir)

out_tif = os.path.join(work_dir, config['Option']['FileName'] + ".tif")


geemap.download_ee_image(
    image=composite,
    filename=out_tif,
    region=roi,
    crs=config['Option']['Crs'],
    scale= int(config['Option']['Scale']),
)
