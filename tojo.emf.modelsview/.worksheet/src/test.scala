object test {

	case class Apa(i: Int);import org.scalaide.worksheet.runtime.library.WorksheetSupport._; def main(args: Array[String])=$execute{;$skip(72); 

	val list = List(1 -> 2, 3 -> 4);System.out.println("""list  : List[(Int, Int)] = """ + $show(list ));$skip(30); 
	val list2 = List(1, 2, 3, 4);System.out.println("""list2  : List[Int] = """ + $show(list2 ));$skip(50); 
	val list3 = List(Apa(1), Apa(2), Apa(3), Apa(4));System.out.println("""list3  : List[test.Apa] = """ + $show(list3 ));$skip(35); val res$0 = 


	list map { case (a, b) => a+b };System.out.println("""res0: List[Int] = """ + $show(res$0));$skip(69); val res$1 = 
	// list2 map { case (a, b) => a+b }
	list3 map { case Apa(a) => a };System.out.println("""res1: List[Int] = """ + $show(res$1))}



}
