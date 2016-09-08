package com.ohchang.controller

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod}

/**
  * @author OhChang Kwon(ohchang.kwon@navercorp.com)
  */
@Controller
@RequestMapping(path = Array("/"))
class IndexController {
  @RequestMapping(method = Array(RequestMethod.GET))
  def index(model:Model): String = {
    model.addAttribute("isHome", true);
    return "index"
  }
}
