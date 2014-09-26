get "/searchBiz", forward: "/search.groovy"
post "/searchBiz", forward: "/search.groovy"


get "/", forward: "/WEB-INF/pages/index.gtpl"

get "/favicon.ico", redirect: "/images/gaelyk-small-favicon.png"


