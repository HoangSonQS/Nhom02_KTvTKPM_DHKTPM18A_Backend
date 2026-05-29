package iuh.fit.se.config.datasource;

import com.zaxxer.hikari.HikariDataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Configuration
public class ReadWriteRoutingDataSource {

    @Bean
    @ConfigurationProperties("spring.datasource")
    DataSourceProperties writeDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    HikariDataSource writeDataSource(DataSourceProperties writeDataSourceProperties) {
        return writeDataSourceProperties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @ConfigurationProperties("app.datasource.read")
    ReadReplicaProperties readReplicaProperties() {
        return new ReadReplicaProperties();
    }

    @Bean
    @Primary
    DataSource dataSource(HikariDataSource writeDataSource, ReadReplicaProperties readReplicaProperties) {
        var routingDataSource = new TransactionAwareRoutingDataSource();
        Map<Object, Object> targets = new HashMap<>();
        targets.put(Route.WRITE, writeDataSource);

        var replicas = readReplicaProperties.getReplicas();
        for (int i = 0; i < replicas.size(); i++) {
            var replica = replicas.get(i);
            var dataSource = new HikariDataSource();
            dataSource.setJdbcUrl(replica.getUrl());
            dataSource.setUsername(replica.getUsername());
            dataSource.setPassword(replica.getPassword());
            dataSource.setPoolName("SeBookReadPool-" + (i + 1));
            dataSource.setMaximumPoolSize(readReplicaProperties.getHikari().getMaximumPoolSize());
            dataSource.setMinimumIdle(readReplicaProperties.getHikari().getMinimumIdle());
            targets.put(Route.read(i), dataSource);
        }

        routingDataSource.setDefaultTargetDataSource(writeDataSource);
        routingDataSource.setTargetDataSources(targets);
        routingDataSource.setReplicaCount(replicas.size());
        return routingDataSource;
    }

    static final class TransactionAwareRoutingDataSource extends AbstractRoutingDataSource {
        private final AtomicInteger counter = new AtomicInteger();
        private int replicaCount;

        void setReplicaCount(int replicaCount) {
            this.replicaCount = replicaCount;
        }

        @Override
        protected Object determineCurrentLookupKey() {
            if (!TransactionSynchronizationManager.isCurrentTransactionReadOnly() || replicaCount == 0) {
                return Route.WRITE;
            }
            int index = Math.floorMod(counter.getAndIncrement(), replicaCount);
            return Route.read(index);
        }
    }

    public static final class ReadReplicaProperties {
        private List<Replica> replicas = new ArrayList<>();
        private Hikari hikari = new Hikari();

        public List<Replica> getReplicas() {
            return replicas;
        }

        public void setReplicas(List<Replica> replicas) {
            this.replicas = replicas == null ? new ArrayList<>() : replicas;
        }

        public Hikari getHikari() {
            return hikari;
        }

        public void setHikari(Hikari hikari) {
            this.hikari = hikari == null ? new Hikari() : hikari;
        }
    }

    public static final class Replica {
        private String url;
        private String username;
        private String password;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static final class Hikari {
        private int maximumPoolSize = 20;
        private int minimumIdle = 5;

        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        public int getMinimumIdle() {
            return minimumIdle;
        }

        public void setMinimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
        }
    }

    record Route(String key) {
        static final Route WRITE = new Route("WRITE");

        static Route read(int index) {
            return new Route("READ_" + index);
        }
    }
}
