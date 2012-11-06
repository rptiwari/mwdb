package edu.mwdb.project;

public class Run1B {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		UserCompLDA1 t1b = new UserCompLDA1();
		String personNum = "1632672";
		try {
			t1b.doLatentSemantics(personNum);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
