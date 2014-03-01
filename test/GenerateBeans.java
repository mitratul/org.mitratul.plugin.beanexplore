public class GenerateBeans {
	public static void main(String[] args) {
		String templateP1 = "<bean id=\"OutputHelper";
		String templateP2 = "\" class=\"com.mkyong.output.OutputHelper\">"
				+ "\n\t<property name=\"outputGenerator\" >"
				+ "\n\t\t<ref bean=\"CsvOutputGenerator\"/>"
				+ "\n\t</property>" + "\n</bean>";
		
		for (int i = 5000; i < 10000; i ++) {
			System.out.println(templateP1 + i + templateP2);
		}
	}
}
