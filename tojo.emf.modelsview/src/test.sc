object test {

	case class Apa(i: Int)

	val list = List(1 -> 2, 3 -> 4)           //> list  : List[(Int, Int)] = List((1,2), (3,4))
	val list2 = List(1, 2, 3, 4)              //> list2  : List[Int] = List(1, 2, 3, 4)
	val list3 = List(Apa(1), Apa(2), Apa(3), Apa(4))
                                                  //> list3  : List[test.Apa] = List(Apa(1), Apa(2), Apa(3), Apa(4))


	list map { case (a, b) => a+b }           //> res0: List[Int] = List(3, 7)
	// list2 map { case (a, b) => a+b }
	list3 map { case Apa(a) => a }            //> res1: List[Int] = List(1, 2, 3, 4)



}