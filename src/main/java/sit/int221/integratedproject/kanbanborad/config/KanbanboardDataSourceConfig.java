package sit.int221.integratedproject.kanbanborad.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "sit.int221.integratedproject.kanbanborad.repositories.kanbanboard",
        entityManagerFactoryRef = "kanbanboardEntityManagerFactory",
        transactionManagerRef = "kanbanboardTransactionManager"
)
public class KanbanboardDataSourceConfig {

    @Primary
    @Bean(name = "kanbanboardDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.kanbanboard")
    public DataSource kanbanboardDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean(name = "kanbanboardEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean kanbanboardEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("kanbanboardDataSource") DataSource kanbanboardDataSource) {
        return builder
                .dataSource(kanbanboardDataSource)
                .packages("sit.int221.integratedproject.kanbanborad.entities")
                .persistenceUnit("kanbanboard")
                .build();
    }

    @Primary
    @Bean(name = "kanbanboardTransactionManager")
    public PlatformTransactionManager kanbanboardTransactionManager(
            @Qualifier("kanbanboardEntityManagerFactory") EntityManagerFactory kanbanboardEntityManagerFactory) {
        return new JpaTransactionManager(kanbanboardEntityManagerFactory);
    }
}