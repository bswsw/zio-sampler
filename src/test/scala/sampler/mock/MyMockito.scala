package sampler.mock

import org.mockito.BDDMockito._
import org.mockito.Mockito.mock

trait MyMockito {
  
}

object MyMockito {
  val myService: MyService = mock(classOf[MyService])
  `given`(myService.execute("")).willReturn(" executed")
}

trait MyService {
  def execute(str: String): String = str + " executed"
}