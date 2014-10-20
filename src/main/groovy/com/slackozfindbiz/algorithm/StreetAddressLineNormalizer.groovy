package com.slackozfindbiz.algorithm

class StreetAddressLineNormalizer {

    private final static def STREET_NAME_ABBREV = [
            'accs':'access',
            'ally':'alley',
            'alwy':'alleyway',
            'ambl':'amble',
            'ancg':'anchorage',
            'app':'approach',
            'arc':'arcade',
            'art':'artery',
            'ave':'avenue',
            'basn':'basin',
            'bch':'beach',
            'bend':'bend',
            'blk':'block',
            'bvd':'boulevard',
            'brce':'brace',
            'brae':'brae',
            'brk':'break',
            'bdge':'bridge',
            'bdwy':'broadway',
            'brow':'brow',
            'bypa':'bypass',
            'bywy':'byway',
            'caus':'causeway',
            'ctr':'centre',
            'cnwy':'centreway',
            'ch':'chase',
            'cir':'circle',
            'clt':'circlet',
            'cct':'circuit',
            'crcs':'circus',
            'cl':'close',
            'clde':'colonnade',
            'cmmn':'common',
            'con':'concourse',
            'cps':'copse',
            'cnr':'corner',
            'cso':'corso',
            'ct':'court',
            'ctyd':'courtyard',
            'cove':'cove',
            'cres':'crescent',
            'crst':'crest',
            'crss':'cross',
            'crsg':'crossing',
            'crd':'crossroad',
            'cowy':'crossway',
            'cuwy':'cruiseway',
            'cds':'cul-de-sac',
            'cttg':'cutting',
            'dale':'dale',
            'dell':'dell',
            'devn':'deviation',
            'dip':'dip',
            'dstr':'distributor',
            'dr':'drive',
            'drwy':'driveway',
            'edge':'edge',
            'elb':'elbow',
            'end':'end',
            'ent':'entrance',
            'esp':'esplanade',
            'est':'estate',
            'exp':'expressway',
            'extn':'extension',
            'fawy':'fairway',
            'ftrk':'fire track',
            'fitr':'firetrail',
            'flat':'flat',
            'folw':'follow',
            'ftwy':'footway',
            'fshr':'foreshore',
            'form':'formation',
            'fwy':'freeway',
            'frnt':'front',
            'frtg':'frontage',
            'gap':'gap',
            'gdn':'garden',
            'gdns':'gardens',
            'gte':'gate',
            'gtes':'gates',
            'gld':'glade',
            'glen':'glen',
            'gra':'grange',
            'grn':'green',
            'grnd':'ground',
            'gr':'grove',
            'gly':'gully',
            'hts':'heights',
            'hrd':'highroad',
            'hwy':'highway',
            'hill':'hill',
            'intg':'interchange',
            'intn':'intersection',
            'jnc':'junction',
            'key':'key',
            'ldg':'landing',
            'lane':'lane',
            'lnwy':'laneway',
            'lees':'lees',
            'line':'line',
            'link':'link',
            'lt':'little',
            'lkt':'lookout',
            'loop':'loop',
            'lwr':'lower',
            'mall':'mall',
            'mndr':'meander',
            'mew':'mew',
            'mews':'mews',
            'mwy':'motorway',
            'mt':'mount',
            'nook':'nook',
            'otlk':'outlook',
            'pde':'parade',
            'park':'park',
            'pkld':'parklands',
            'pkwy':'parkway',
            'part':'part',
            'pass':'pass',
            'path':'path',
            'phwy':'pathway',
            'piaz':'piazza',
            'pl':'place',
            'plat':'plateau',
            'plza':'plaza',
            'pkt':'pocket',
            'pnt':'point',
            'port':'port',
            'prom':'promenade',
            'quad':'quad',
            'qdgl':'quadrangle',
            'qdrt':'quadrant',
            'qy':'quay',
            'qys':'quays',
            'rmbl':'ramble',
            'ramp':'ramp',
            'rnge':'range',
            'rch':'reach',
            'res':'reserve',
            'rest':'rest',
            'rtt':'retreat',
            'ride':'ride',
            'rdge':'ridge',
            'rgwy':'ridgeway',
            'rowy':'right of way',
            'ring':'ring',
            'rise':'rise',
            'rvr':'river',
            'rvwy':'riverway',
            'rvra':'riviera',
            'rd':'road',
            'rds':'roads',
            'rdsd':'roadside',
            'rdwy':'roadway',
            'rnde':'ronde',
            'rsbl':'rosebowl',
            'rty':'rotary',
            'rnd':'round',
            'rte':'route',
            'row':'row',
            'rue':'rue',
            'run':'run',
            'swy':'service way',
            'sdng':'siding',
            'slpe':'slope',
            'snd':'sound',
            'spur':'spur',
            'sq':'square',
            'strs':'stairs',
            'shwy':'state highway',
            'stps':'steps',
            'stra':'strand',
            'st':'street',
            'strp':'strip',
            'sbwy':'subway',
            'tarn':'tarn',
            'tce':'terrace',
            'thor':'thoroughfare',
            'tlwy':'tollway',
            'top':'top',
            'tor':'tor',
            'twrs':'towers',
            'trk':'track',
            'trl':'trail',
            'trlr':'trailer',
            'tri':'triangle',
            'tkwy':'trunkway',
            'turn':'turn',
            'upas':'underpass',
            'upr':'upper',
            'vale':'vale',
            'vdct':'viaduct',
            'view':'view',
            'vlls':'villas',
            'vsta':'vista',
            'wade':'wade',
            'walk':'walk',
            'wkwy':'walkway',
            'way':'way',
            'whrf':'wharf',
            'wynd':'wynd',
            'yard':'yard'
        ]

    static def normalize(original) {
        new StreetAddressLineNormalizer(original)
            .removeNonAlphaNumeric()
            .compactSpaces()
            .expandCommonAbbrev()
            .reverse()
            .truncateAfterEndOfNumber()
            .toString()
    }
    
    private String normalized
    
    private StreetAddressLineNormalizer(original) {
        normalized = original.trim()
    }
    
    private def removeNonAlphaNumeric() {
        normalized = normalized.replaceAll('[^ a-zA-Z0-9]', ' ')
        this
    }
    
    private def expandCommonAbbrev() {
        def tokens = normalized.split(' ') as List
        def expandedTokens = tokens.collect { token ->
            def expanded = STREET_NAME_ABBREV.get(token)
            (expanded != null) ? expanded : token
        }
        normalized = expandedTokens.join(' ') 
        this
    }
    
    private def compactSpaces() {
        normalized = normalized.replaceAll('\\s+', ' ')
        this
    }
    
    private def reverse() {
        normalized = normalized.reverse()
        this
    }
    
    private def truncateAfterEndOfNumber() {
        if (normalized) {
            def regexMatch = normalized.find(~/^(.*?[0-9])[^0-9]/) { full, group -> group }
            normalized = regexMatch ? regexMatch : normalized
        }
        
        this
    }
    
    String toString() {
        normalized
    }


}
