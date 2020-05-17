package utils;

import model.User;

public class Test {
    public static void main(final String[] argz) throws Exception {
        final User user = new User();
        user.setOptions(141);

        for (final UOpts o : UOpts.values()) {
            System.out.println(o.name()+" >> " + o.is(user));
//            if (TextUtils.rnd.nextInt() % 2 == 0) {
                o.set(user);
//                System.out.print(" apply '" + o.name() + ": " + user.getOptions());
//            }
//            System.out.print("; has ? " + o.is(user) + "\n");
        }
    }
}
