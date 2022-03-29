import org.jetbrains.annotations.NotNull;

/**
 * В теле класса решения разрешено использовать только финальные переменные типа RegularInt.
 * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
 *
 * @author : Shaldin Vsevolod
 */
public class Solution implements MonotonicClock {
    private final RegularInt c1_1 = new RegularInt(0);
    private final RegularInt c1_2 = new RegularInt(0);
    private final RegularInt c1_3 = new RegularInt(0);

    private final RegularInt c2_1 = new RegularInt(0);
    private final RegularInt c2_2 = new RegularInt(0);
    private final RegularInt c2_3 = new RegularInt(0);

    @Override
    public void write(@NotNull Time time) {
        if (time.compareTo(new Time(c2_1.getValue(), c2_2.getValue(), c2_3.getValue())) >= 0) {
            c2_3.setValue(time.getD3());
            c2_2.setValue(time.getD2());
            c2_1.setValue(time.getD1());
        }

        c1_3.setValue(c2_3.getValue());
        c1_2.setValue(c2_2.getValue());
        c1_1.setValue(c2_1.getValue());
    }

    @NotNull
    @Override
    public Time read() {
        final RegularInt r1_1 = new RegularInt(c1_1.getValue());
        final RegularInt r1_2 = new RegularInt(c1_2.getValue());
        final RegularInt r1_3 = new RegularInt(c1_3.getValue());

        final RegularInt r2_3 = new RegularInt(c2_3.getValue());
        final RegularInt r2_2 = new RegularInt(c2_2.getValue());
        final RegularInt r2_1 = new RegularInt(c2_1.getValue());

        if (r1_1.getValue() == r2_1.getValue()) {
            if (r1_2.getValue() == r2_2.getValue()) {
                if (r1_3.getValue() == r2_3.getValue()) {
                    return new Time(r1_1.getValue(), r1_2.getValue(), r1_3.getValue());
                } else {
                    return new Time(r1_1.getValue(), r1_2.getValue(), Math.max(r1_3.getValue(), r2_3.getValue()));
                }
            } else {
                return new Time(r1_1.getValue(), Math.max(r1_2.getValue(), r2_2.getValue()), 0);
            }
        } else {
            return new Time(Math.max(r1_1.getValue(), r2_1.getValue()), 0, 0);
        }
    }
}
