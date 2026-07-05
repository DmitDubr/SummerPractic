package validate

import (
	"testing"

	"github.com/dimbass/summerpractic/climbing/backend/internal/api"
)

func TestClientContactsPhone(t *testing.T) {
	err := ClientContacts(api.ClientContacts{Name: "Иван", Phone: "+79001234567"})
	if err != nil {
		t.Fatalf("expected valid phone, got %v", err)
	}
	err = ClientContacts(api.ClientContacts{Name: "Иван", Phone: "8900"})
	if err == nil {
		t.Fatal("expected invalid phone")
	}
}

func TestEquipmentRentalRequiresItem(t *testing.T) {
	err := Equipment(api.EquipmentChoice{Mode: "RENTAL"})
	if err == nil {
		t.Fatal("expected error for empty rental")
	}
	err = Equipment(api.EquipmentChoice{Mode: "RENTAL", RentalShoes: true})
	if err != nil {
		t.Fatalf("expected ok, got %v", err)
	}
}
