web: target/universal/stage/bin/corespring -Dhttp.port=${PORT} -Dlogger.file=${ENV_LOGGER}
worker: java ${JAVA_OPTS} -cp "target/universal/stage/lib/*" scheduler.RabbitMQ