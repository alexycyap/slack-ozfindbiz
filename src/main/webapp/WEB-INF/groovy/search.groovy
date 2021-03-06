import com.slackozfindbiz.service.SearchService
import com.slackozfindbiz.config.ConfigLoader
import com.slackozfindbiz.utils.IOUtils


def config = ConfigLoader.getConfig(context)
def searchService = new SearchService(config)

def output = searchService.search(params, request.remoteAddr, request.session.id)
IOUtils.streamJsonStr("{ \"text\" : \"${output}\" }", response, sout)

 
