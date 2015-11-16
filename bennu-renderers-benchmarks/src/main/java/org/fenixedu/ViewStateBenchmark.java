package org.fenixedu;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import pt.ist.fenixWebFramework.renderers.components.state.IViewState;
import pt.ist.fenixWebFramework.renderers.components.state.ViewState;
import pt.ist.fenixWebFramework.renderers.model.MetaObjectFactory;
import pt.ist.fenixWebFramework.renderers.model.SchemaFactory;

@Fork(2)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class ViewStateBenchmark {

    private ViewState viewState;
    private String serialized;

    @Setup
    public void setup() throws IOException {
        this.viewState = new ViewState();
        TestBean bean = new TestBean();
        bean.setName("xpto");
        bean.setAge(21);
        List<String> friends = new ArrayList<>();
        friends.add("ptox");
        friends.add("foo");
        friends.add("bar");
        bean.setFriends(friends);
        viewState.setMetaObject(MetaObjectFactory.createObject(bean, SchemaFactory.create(bean)));

        this.serialized = ViewState.encodeToBase64(Collections.singletonList(viewState));
    }

    @Benchmark
    public List<IViewState> deserialize() throws ClassNotFoundException, IOException {
        return ViewState.decodeFromBase64(serialized);
    }

    @Benchmark
    public String serialize() throws IOException {
        return ViewState.encodeToBase64(Collections.singletonList(viewState));
    }

    private static final class TestBean implements Serializable {

        private static final long serialVersionUID = -8022141966441804782L;

        private String name;
        private int age;
        private List<String> friends;

        public TestBean() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public List<String> getFriends() {
            return friends;
        }

        public void setFriends(List<String> friends) {
            this.friends = friends;
        }

    }

}
