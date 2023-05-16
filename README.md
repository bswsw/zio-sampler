# zio-sampler

## kafka

### docker compose

```shell
docker-compose up -d

docker exec -it broker bash
```

### 카프카 명령어

```shell
kafka-topics \
  --bootstrap-server localhost:9092 \
  --topic test-topic \
  --create

kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic test-topic
```