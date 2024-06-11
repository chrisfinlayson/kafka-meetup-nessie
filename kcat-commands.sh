topics=("logins" "accounts" "transactions")

for topic in "${topics[@]}"
do
  kcat -b localhost:9092 -t $topic -u | jq
done

kcat -b localhost:9092 -t logins -u | jq
kcat -b localhost:9092 -t accounts -u | jq
kcat -b localhost:9092 -t transactions -u | jq


kcat -b localhost:9092 -G OrderEventStream OrderLineEventStream ProductEventStream -u | jq