# ElasticSearch Address TIGER Data Schema

Data is stored in Elasticsearch, by convention in an index called `census` with type `addrfeat`. Data format is GeoJSON objects with the following structure:

* type: "Feature"
* geometry: GeoJSON geometry representation
* properties: Fields that include address information from the TIGER ADDRFEAT data set, in particular:
	* FULLNAME: Street name
	* LFROMHN: Start house number, left side
	* LTOHN: End house number, left side
	* RFROMHN: Start house number, right side
	* RTOHN: End house number, right side
	* ZIPL: Zip code, left side
	* ZIPR: Zip code, right side

Example:

```json
{
  "type": "Feature",
  "geometry": {
    "type": "LineString",
    "coordinates": [
       [
         -91.33126,
          34.29961999999999
       ],
       [
         -91.33137999999998,
         34.29961999999999
       ],
       [
         -91.33173999999998,
          34.29961999999999
       ],
       [
         -91.33185999999999,
          34.29961999999999
       ]
     ]
  },
  "properties": {
     "ARIDL": "4003951555205",
     "ARIDR": "",
     "EDGE_MTFCC": "S1400",
     "FULLNAME": "Roth Prairie Rd",
     "LFROMHN": "101",
     "LFROMTYP": "",
     "LINEARID": "110378466099",
     "LTOHN": "103",
     "LTOTYP": "",
     "OFFSETL": "N",
     "OFFSETR": "N",
     "PARITYL": "O",
     "PARITYR": "",
     "PLUS4L": "",
     "PLUS4R": "",
     "RFROMHN": "",
     "RFROMTYP": "",
     "ROAD_MTFCC": "S1400",
     "RTOHN": "",
     "RTOTYP": "",
     "TFIDL": "208595926",
     "TFIDR": "208595636",
     "TLID": "11961970",
     "ZIPL": "72160",
     "ZIPR": "",
     "STATE": "AR"
  }
}
```

A typical search will return records in the following format when using ElasticSearch's REST API:

```json
{
  "_index": "census",
  "_type": "addrfeat",
  "_id": "AU0fPkzMfUUh7D1HEp9O",
  "_score": 1,
  "_source": {
  "type": "Feature",
    "geometry": {
      "type": "LineString",
      "coordinates": [
         [
           -91.33126,
            34.29961999999999
         ],
         [
           -91.33137999999998,
           34.29961999999999
         ],
         [
           -91.33173999999998,
            34.29961999999999
         ],
         [
           -91.33185999999999,
            34.29961999999999
         ]
       ]
    },
    "properties": {
       "ARIDL": "4003951555205",
       "ARIDR": "",
       "EDGE_MTFCC": "S1400",
       "FULLNAME": "Roth Prairie Rd",
       "LFROMHN": "101",
       "LFROMTYP": "",
       "LINEARID": "110378466099",
       "LTOHN": "103",
       "LTOTYP": "",
       "OFFSETL": "N",
       "OFFSETR": "N",
       "PARITYL": "O",
       "PARITYR": "",
       "PLUS4L": "",
       "PLUS4R": "",
       "RFROMHN": "",
       "RFROMTYP": "",
       "ROAD_MTFCC": "S1400",
       "RTOHN": "",
       "RTOTYP": "",
       "TFIDL": "208595926",
       "TFIDR": "208595636",
       "TLID": "11961970",
       "ZIPL": "72160",
       "ZIPR": "",
       "STATE": "AR"
    }
  }

}
```

## Data creation

The census geocoder uses Elasticsearch synonyms to resolve abbreviations (i.e. St for Street, or MD for Maryland).
The synonyms.txt file with the synonyms definition must be installed in every node in the cluster, in the same directory as the elasticsearch.yml configuration file.

TIGER data can be loaded using the [loader](https://github.com/cfpb/grasshopper-loader).

After loading the data the following can be run to account for the synonyms:

```shell
# close the index
curl -XPOST [ip]:9200/census/_close?pretty=1
```

```shell
# add the synonym analyzer and filter to the settings
curl -XPUT [ip]:9200/census/_settings?pretty=1 -d '
{
  "analysis" : {
    "analyzer" : {
      "synonym" : {
        "tokenizer" : "whitespace",
        "filter" : ["synonym"]
      }
    },
    "filter" : {
      "synonym" : {
        "type" : "synonym",
        "synonyms_path": "synonyms.txt",
        "ignore_case": true 
      }
    }
  }
}'
```

*update the necessary properties, example of FULLNAME and STATE are below*

```shell
# update the correct properties in the mapping to use the analyzer
# ignore_conflicts needed to avoid MergeMappingException[Merge failed with failures {[mapper [properties.property] has different index_analyzer]}]
curl -XPUT [ip]:9200/census/_mapping/addrfeat?pretty=1&ignore_conflicts=true -d '
{
  "addrfeat": {
    "properties": {
      "properties": {
        "properties": {
          "FULLNAME": {
            "type": "string",
            "analyzer": "synonym"
          },
          "STATE": {
            "type": "string",
            "analyzer": "synonym"
          }
        }
      }
    }
  }
}'
```

```shell
# verify the mapping is correct
curl -XGET http://[ip]:9200/census/_mapping?pretty=1
```

within the response you should see

```json
"FULLNAME": {
  "type": "string",
  "search_analyzer": "synonym"
},
"STATE": {
  "type": "string",
  "search_analyzer": "synonym"
},
```

then you can

```shell
# run some tests
# mixed case spelled out
curl -XGET http://[ip]:9200/census/_search?pretty=1
{
    "query": {
        "match": {
            "properties.FULLNAME": "Road"
        }
    }
}

# lowercase abbrevated
GET /census/_search?pretty=1
{
    "query": {
        "match": {
            "properties.FULLNAME": "rd"
        }
    }
}
```